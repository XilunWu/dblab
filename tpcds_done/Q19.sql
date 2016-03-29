
SELECT  i_brand_id brand_id, i_brand brand, i_manufact_id, i_manufact,
 	SUM(ss_ext_sales_price) ext_price
 FROM date_dim, store_sales, item,customer,customer_address,store
 WHERE d_date_sk = ss_sold_date_sk
   AND ss_item_sk = i_item_sk
   AND i_manager_id=7
   AND d_moy=11
   AND d_year=1999
   AND ss_customer_sk = c_customer_sk 
   AND c_current_addr_sk = ca_address_sk
   AND SUBSTR(ca_zip,1,5) <> SUBSTR(s_zip,1,5) 
   AND ss_store_sk = s_store_sk 
 GROUP BY brand
      ,brand_id
      ,i_manufact_id
      ,i_manufact
 ORDER BY ext_price DESC
         ,brand
         ,brand_id
         ,i_manufact_id
         ,i_manufact
LIMIT 100 ;


