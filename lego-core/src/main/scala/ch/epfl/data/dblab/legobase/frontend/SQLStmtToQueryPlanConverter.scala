package ch.epfl.data
package dblab.legobase
package frontend

import utils._
import Numeric.Implicits._
import Ordering.Implicits._
import schema._
import storagemanager._
import ch.epfl.data.dblab.legobase.queryengine.push._
import scala.collection.mutable.HashMap
import sc.pardis.shallow.CaseClassRecord
import scala.language.implicitConversions

// TODO: The rest we should not need when we generalize
import tpch._
import scala.reflect._
import scala.reflect.runtime.universe._
import scala.reflect.runtime.currentMirror

class SQLTreeToQueryPlanConverter(schema: Schema) {
  val tableMap = new HashMap[String, Array[_]]()
  var currBindVar: CaseClassRecord = _

  def loadRelations(sqlTree: SelectStatement) {
    val sqlRelations = sqlTree.relations;
    val schemaRelations = sqlRelations.map(r => schema.findTable(r match {
      case t: Table    => t.name
      case s: Subquery => s.alias
    }))
    schemaRelations.foreach(tn => tableMap += tn.name -> {
      // TODO: Generalize
      Loader.loadTable[LINEITEMRecord](tn)(classTag[LINEITEMRecord])
    })
  }

  def parseNumericExpression[A: TypeTag](e: Expression): A = (e match {
    case FieldIdent(_, name, _) => currBindVar.getField(name).get
    case DateLiteral(v)         => v
    case FloatLiteral(v)        => v
    case IntLiteral(v)          => v
    case _                      => parseExpression(e)
  }).asInstanceOf[A]

  /**
   * This method receives two numbers and makes sure that both number have the same type.
   * If their type is different, it will upcast the number with lower type to the type
   * of the other number.
   */
  def promoteNumbers[A: TypeTag, B: TypeTag](n1: A, n2: B): (Any, Any) = {
    val IntType = typeTag[Int]
    val FloatType = typeTag[Float]
    val DoubleType = typeTag[Double]
    (
      (typeTag[A], typeTag[B]) match {
        case (x, y) if x == y        => n1 -> n2
        case (IntType, DoubleType)   => n1.asInstanceOf[Int].toDouble -> n2
        case (DoubleType, FloatType) => n1 -> n2.asInstanceOf[Float].toDouble
        case (x, y)                  => throw new Exception(s"Does not know how to find the common type for $x and $y")
      }).asInstanceOf[(Any, Any)]
  }

  def computeOrderingExpression[A: TypeTag, B: TypeTag](e1: Expression, e2: Expression, op: (Ordering[Any]#Ops, Any) => Boolean): Boolean = {
    implicit val numericA = getNumVal[A]
    implicit val numericB = getNumVal[B]
    val n1 = parseNumericExpression[A](e1)
    val n2 = parseNumericExpression[B](e2)
    val (pn1, pn2) = promoteNumbers[A, B](n1, n2)
    val orderingA = numericA.asInstanceOf[Ordering[Any]]
    val opn1 = new orderingA.Ops(pn1)
    op(opn1, pn2)
  }

  def computeNumericExpression[A: TypeTag, B: TypeTag](e1: Expression, e2: Expression, op: (Numeric[Any]#Ops, Any) => Any): Any = {
    implicit val numericA = getNumVal[A]
    implicit val numericB = getNumVal[B]
    val n1 = parseNumericExpression[A](e1)
    val n2 = parseNumericExpression[B](e2)
    val (pn1, pn2) = promoteNumbers[A, B](n1, n2)
    val numericAAny = numericA.asInstanceOf[Numeric[Any]]
    val opn1 = new numericAAny.Ops(pn1)
    op(opn1, pn2)
  }

  /*def typeTagToNumeric[A](tp: TypeTag[A]): Numeric[A] = (tp match {
    case x if x == typeTag[Int] => implicitly[Numeric[Int]]
  }).asInstanceOf[Numeric[A]]*/

  //sdef typeNum[T](tp: TypeTag[T]) = tp.asInstanceOf[TypeTag[NumType]]

  def getNumVal[T](implicit tp: TypeTag[T]): Numeric[T] = (tp match {
    case x if x == typeTag[Int]    => implicitly[Numeric[Int]]
    case x if x == typeTag[Double] => implicitly[Numeric[Double]]
    case x if x == typeTag[Float]  => implicitly[Numeric[Float]]
  }).asInstanceOf[Numeric[T]]

  def parseExpression[A: TypeTag](e: Expression): A = (e match {
    // Literals
    case FieldIdent(_, _, _) | IntLiteral(_) | DateLiteral(_) | FloatLiteral(_) =>
      parseNumericExpression(e)
    // Arithmetic Operators
    case Add(left, right) =>
      computeNumericExpression(left, right, (x, y) => x + y)(left.tp, right.tp)
    case Subtract(left, right) =>
      computeNumericExpression(left, right, (x, y) => x - y)(left.tp, right.tp)
    // Logical Operators
    case And(left, right) =>
      parseExpression[Boolean](left) && parseExpression[Boolean](right)
    // Comparison Operators
    case GreaterOrEqual(left, right) =>
      computeOrderingExpression(left, right, (x, y) => x >= y)(left.tp, right.tp)
    case LessOrEqual(left, right) =>
      computeOrderingExpression(left, right, (x, y) => x <= y)(left.tp, right.tp)
    case LessThan(left, right) =>
      computeOrderingExpression(left, right, (x, y) => x < y)(left.tp, right.tp)
  }).asInstanceOf[A]

  def parseWhereClauses(e: Option[Expression], parentOp: Operator[_]): Operator[_] = e match {
    case Some(expr) =>
      new SelectOp(parentOp)(arg => {
        currBindVar = arg.asInstanceOf[CaseClassRecord];
        parseExpression(expr).asInstanceOf[Boolean]
      })
    case None => parentOp
  }

  def convertOperators(sqlTree: SelectStatement): Operator[_] = {
    val scanOps = tableMap.map({ case (tableName, tab) => (tableName, new ScanOp(tab)) })
    val scanOp = scanOps.find(s => s._1 == "LINEITEM").get._2 // TODO: Generalize
    val selectOp = parseWhereClauses(sqlTree.where, scanOp)
    new PrintOp(selectOp)(t => {} /*println(t)*/ , -1)
  }

  def convert(sqlTree: SelectStatement) {
    val tableMap = loadRelations(sqlTree)
    val operatorTree = convertOperators(sqlTree)
    // Execute tree
    for (i <- 0 until Config.numRuns) {
      Utilities.time({
        operatorTree.open
        operatorTree.next
      }, "Query")
    }
  }
}
