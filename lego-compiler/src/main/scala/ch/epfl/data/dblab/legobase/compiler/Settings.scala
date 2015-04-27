package ch.epfl.data
package dblab.legobase
package compiler

/**
 * Handles the setting parameters which are passed as the main arguments of the program.
 */
class Settings(val args: List[String]) {
  val SUPPORTED_CS = (1 to 22).toList
  val LARGE_OUTPUT_QUERIES = List(10, 11, 16, 20)

  def isLargeOutputQuery(tpchQuery: Int) = LARGE_OUTPUT_QUERIES.contains(tpchQuery)

  def validate(targetIsC: Boolean, tpchQuery: Int): Unit = {
    for (arg <- args.filter(a => a.startsWith("+") || a.startsWith("-")).filter(arg => !Settings.ALL_SETTINGS.exists(_.fullFlagName == arg))) {
      System.out.println(s"${Console.YELLOW}Warning${Console.RESET}: flag $arg is not defined!")
    }
    if (!hashMapLowering && (setToArray || setToLinkedList || containerFlattenning))
      throw new Exception("It's impossible to lower Sets without lowering HashMap and MultiMap!")
    if (hashMapLowering && hashMapNoCollision)
      throw new Exception(s"${HashMapToArraySetting.flagName} and ${HashMapToSetSetting.flagName} cannot be chained together.")
    if ((columnStore || partitioning) && (!SUPPORTED_CS.contains(tpchQuery)))
      throw new Exception(s"${ColumnStoreSetting.flagName} and ${ArrayPartitioningSetting.flagName} only work for the Queries ${SUPPORTED_CS.mkString(" & ")} for the moment!")
    if (!hasSetting(LargeOutputHoistingSetting) && isLargeOutputQuery(tpchQuery)) {
      System.out.println(s"${Console.YELLOW}Warning${Console.RESET}: The queries ${LARGE_OUTPUT_QUERIES.mkString("Q", ", Q", "")} should have ${LargeOutputHoistingSetting.flagName} flag. Automatically enabled!")
    } else if (hasSetting(LargeOutputHoistingSetting) && !isLargeOutputQuery(tpchQuery)) {
      System.out.println(s"${Console.YELLOW}Warning${Console.RESET}: Only the queries ${LARGE_OUTPUT_QUERIES.mkString("Q", ", Q", "")} should have ${LargeOutputHoistingSetting.flagName} flag.")
    }
    if (hasSetting(LargeOutputHoistingSetting) && !targetIsC) {
      throw new Exception(s"${LargeOutputHoistingSetting.flagName} is only available for C Code Generation.")
    }
    if (pointerStore && oldCArrayHandling) {
      throw new Exception(s"${PointerStoreSetting.flagName} and ${CArrayAsStructSetting.flagName} cannot be chained together.")
    }
  }
  @inline def hasSetting(setting: Setting): Boolean = args.exists(_ == setting.fullFlagName)
  // TODO the following methods are not needed
  def hashMapLowering: Boolean = hasSetting(HashMapToSetSetting)
  def setToArray: Boolean = hasSetting(SetToArraySetting)
  def setToLinkedList: Boolean = hasSetting(SetToLinkedListSetting)
  def containerFlattenning: Boolean = hasSetting(ContainerFlattenningSetting)
  def columnStore: Boolean = hasSetting(ColumnStoreSetting)
  def partitioning: Boolean = hasSetting(ArrayPartitioningSetting)
  def hashMapPartitioning: Boolean = hasSetting(HashMapPartitioningSetting)
  def mallocHoisting: Boolean = hasSetting(MallocHoistSetting)
  def constArray: Boolean = hasSetting(ConstantSizeArraySetting)
  def stringCompression: Boolean = hasSetting(StringDictionarySetting)
  def noLetBinding: Boolean = hasSetting(NoLetBindingSetting)
  def ifAggressive: Boolean = hasSetting(IfAggressiveSetting)
  def oldCArrayHandling: Boolean = hasSetting(CArrayAsStructSetting)
  def pointerStore: Boolean = hasSetting(PointerStoreSetting)
  def stringOptimization: Boolean = hasSetting(StringOptimizationSetting)
  def hashMapNoCollision: Boolean = hasSetting(HashMapToArraySetting)
  def largeOutputHoisting(targetIsC: Boolean, tpchQuery: Int): Boolean = !onlyLoading && (hasSetting(LargeOutputHoistingSetting) || (isLargeOutputQuery(tpchQuery) && targetIsC))
  def noFieldRemoval: Boolean = hasSetting(NoFieldRemovalSetting)
  def noSingletonHashMap: Boolean = hasSetting(NoSingletonHashMapSetting)
  def nameIsWithFlag: Boolean = hasSetting(OutputNameWithFlagSetting)
  def onlyLoading: Boolean = hasSetting(OnlyLoaderSetting)

  import Main.Q12SynthesizedExtract
  def isSynthesized: Boolean = args(2) match {
    case Q12SynthesizedExtract(_, _) => true
    case _                           => false
  }
  def queryName: String = args(2)
}

/**
 * Contains list of available settings
 */
object Settings {
  val ALL_SETTINGS = List(HashMapToSetSetting,
    SetToArraySetting,
    SetToLinkedListSetting,
    ContainerFlattenningSetting,
    ColumnStoreSetting,
    PointerStoreSetting,
    ArrayPartitioningSetting,
    HashMapPartitioningSetting,
    MallocHoistSetting,
    ConstantSizeArraySetting,
    StringDictionarySetting,
    NoLetBindingSetting,
    IfAggressiveSetting,
    CArrayAsStructSetting,
    StringOptimizationSetting,
    HashMapToArraySetting,
    LargeOutputHoistingSetting,
    NoFieldRemovalSetting,
    NoSingletonHashMapSetting,
    OutputNameWithFlagSetting,
    OnlyLoaderSetting)
}

/**
 * The main trait for every setting
 *
 * @field prefix the prefix used for a setting used in command line
 * @field flagName the name of a setting flag
 * @field description the description for a setting
 */
sealed trait Setting {
  val prefix: String
  val flagName: String
  val description: String
  def fullFlagName: String = prefix + flagName
}

/**
 * The super class of optimization settings
 * @param flagName the name of the optimization setting
 * @param mainDescription the main part of the description for the optimization setting
 * @param extraDescription the additional description about the optimization setting
 */
abstract class OptimizationSetting(val flagName: String,
                                   val mainDescription: String,
                                   val extraDescription: String = "") extends Setting {
  val prefix: String = "+"
  val description: String = mainDescription + {
    if (extraDescription.length == 0)
      ""
    else
      s" ($extraDescription)"
  }
}

/**
 * The super class of option settings, which are not related to optimization
 * @param flagName the name of the option setting
 * @param desciprtion the description for the option setting
 */
abstract class OptionSetting(val flagName: String, val description: String) extends Setting {
  val prefix: String = "-"
}

/*
 * Available optimization settings
 */
case object HashMapToSetSetting extends OptimizationSetting("hm2set",
  "Lowering HashMap and MultiMap to Array of Set")
case object SetToArraySetting extends OptimizationSetting("set2arr",
  "Lowering Set to Array")
case object SetToLinkedListSetting extends OptimizationSetting("set2ll",
  "Lowering Set to LinkedList")
case object ContainerFlattenningSetting extends OptimizationSetting("cont-flat",
  "Flattening the next field of a container of a record to the record itself")
case object ColumnStoreSetting extends OptimizationSetting("cstore",
  "Column-Store optimization",
  "Not finished yet!")
case object PointerStoreSetting extends OptimizationSetting("bad-rec",
  "Pointer-Store (de)optimization",
  "Deoptimization!")
case object ArrayPartitioningSetting extends OptimizationSetting("part",
  "Partitions an array whenever possible",
  s"Not finished yet! Works only for Q3 and Q6. For Q6 should be combined with ${ColumnStoreSetting.flagName}.")
case object HashMapPartitioningSetting extends OptimizationSetting("hm-part",
  "Converts MultiMaps into partitioned arrays")
case object MallocHoistSetting extends OptimizationSetting("malloc-hoist",
  "Hoists malloc statements outside of the critical path")
case object ConstantSizeArraySetting extends OptimizationSetting("const-arr",
  "Transforms arrays with a small constant size into local variables")
case object StringDictionarySetting extends OptimizationSetting("comprStrings",
  "Creates a dictionary for strings in the loading time and transforms string operations to integer operations")
case object NoLetBindingSetting extends OptimizationSetting("no-let",
  "Removes unnecessary let-bindings from the generated code")
case object IfAggressiveSetting extends OptimizationSetting("if-agg",
  "Rewrites the conditions of if statements into bitwise form instead of the original short-circuiting form",
  "May produce incorrect results in some queries")
case object CArrayAsStructSetting extends OptimizationSetting("old-carr",
  "Handling C arrays as a struct of pointer and length")
case object StringOptimizationSetting extends OptimizationSetting("str-opt",
  "Some optimizations on string operations",
  "Helpful for Q22")
case object HashMapToArraySetting extends OptimizationSetting("hm-no-col",
  "Transforming HashMap without collisions to Array")
case object LargeOutputHoistingSetting extends OptimizationSetting("large-out",
  "If the output is so large, this flag ignores the time for printing",
  "Helpful for Q10, Q11, Q16, Q20")
case object NoFieldRemovalSetting extends OptimizationSetting("no-field-rem",
  "Disables the unnecessary field removal optimization",
  "Deoptimization!")
case object NoSingletonHashMapSetting extends OptimizationSetting("no-sing-hm",
  "Disables the singleton hashmap optimization",
  "Deoptimization!")

/* 
 * Available option settings
 */
case object OutputNameWithFlagSetting extends OptionSetting("name-with-flag",
  "Appends the optimization flags to the name of files")
case object OnlyLoaderSetting extends OptionSetting("only-load",
  "Generates only the loader of a query")
