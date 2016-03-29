
SELECT  a.ca_state state, COUNT(*) cnt
 FROM customer_address a
     ,customer c
     ,store_sales s
     ,date_dim d
     ,item i
 WHERE       a.ca_address_sk = c.c_current_addr_sk
 	AND c.c_customer_sk = s.ss_customer_sk
 	AND s.ss_sold_date_sk = d.d_date_sk
 	AND s.ss_item_sk = i.i_item_sk
 	AND d.d_month_seq = 
 	     (SELECT DISTINCT (d_month_seq) as distinct_d_month_seq
 	      FROM date_dim
               WHERE d_year = 2000
 	        AND d_moy = 2 )
 	AND i.i_current_price > 1.2 * 
             (SELECT AVG(j.i_current_price) 
 	     FROM item j 
 	     WHERE j.i_category = i.i_category)
 GROUP BY a.ca_state
 HAVING COUNT(*) >= 10
 ORDER BY cnt 
 LIMIT 100;


