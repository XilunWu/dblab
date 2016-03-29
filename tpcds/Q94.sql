
SELECT  
   COUNT(distinct ws_order_number) AS "order count"
  ,SUM(ws_ext_ship_cost) AS "total shipping cost"
  ,SUM(ws_net_profit) AS "total net profit"
FROM
   web_sales ws1
  ,date_dim
  ,customer_address
  ,web_site
WHERE
    d_date BETWEEN '1999-5-01' AND 
           (cast('1999-5-01' AS date) + 60 days)
AND ws1.ws_ship_date_sk = d_date_sk
AND ws1.ws_ship_addr_sk = ca_address_sk
AND ca_state = 'TX'
AND ws1.ws_web_site_sk = web_site_sk
AND web_company_name = 'pri'
AND exists (SELECT *
            FROM web_sales ws2
            WHERE ws1.ws_order_number = ws2.ws_order_number
              AND ws1.ws_warehouse_sk <> ws2.ws_warehouse_sk)
AND not exists(SELECT *
               FROM web_returns wr1
               WHERE ws1.ws_order_number = wr1.wr_order_number)
ORDER BY COUNT(distinct ws_order_number)
LIMIT 100;

