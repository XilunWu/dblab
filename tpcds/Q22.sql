
SELECT  i_product_name
             ,i_brand
             ,i_class
             ,i_category
             ,AVG(inv_quantity_on_hand) qoh
       FROM inventory
           ,date_dim
           ,item
           ,warehouse
       WHERE inv_date_sk=d_date_sk
              AND inv_item_sk=i_item_sk
              AND inv_warehouse_sk = w_warehouse_sk
              AND d_month_seq BETWEEN 1212 AND 1212 + 11
       GROUP BY rollup(i_product_name
                       ,i_brand
                       ,i_class
                       ,i_category)
ORDER BY qoh, i_product_name, i_brand, i_class, i_category
LIMIT 100;


