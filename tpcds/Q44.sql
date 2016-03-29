
SELECT  ASceding.rnk, i1.i_product_name best_performing, i2.i_product_name worst_performing
FROM(SELECT *
     FROM (SELECT item_sk,rank() over (ORDER BY rank_col ASc) rnk
           FROM (SELECT ss_item_sk item_sk,AVG(ss_net_profit) rank_col 
                 FROM store_sales ss1
                 WHERE ss_store_sk = 2
                 GROUP BY ss_item_sk
                 HAVING AVG(ss_net_profit) > 0.9*(SELECT AVG(ss_net_profit) rank_col
                                                  FROM store_sales
                                                  WHERE ss_store_sk = 2
                                                    AND ss_hdemo_sk is null
                                                  GROUP BY ss_store_sk))V1)V11
     WHERE rnk  < 11) ASceding,
    (SELECT *
     FROM (SELECT item_sk,rank() over (ORDER BY rank_col DESC) rnk
           FROM (SELECT ss_item_sk item_sk,AVG(ss_net_profit) rank_col
                 FROM store_sales ss1
                 WHERE ss_store_sk = 2
                 GROUP BY ss_item_sk
                 HAVING AVG(ss_net_profit) > 0.9*(SELECT AVG(ss_net_profit) rank_col
                                                  FROM store_sales
                                                  WHERE ss_store_sk = 2
                                                    AND ss_hdemo_sk is null
                                                  GROUP BY ss_store_sk))V2)V21
     WHERE rnk  < 11) DESCending,
item i1,
item i2
WHERE ASceding.rnk = DESCending.rnk 
  AND i1.i_item_sk=asceding.item_sk
  AND i2.i_item_sk=descending.item_sk
ORDER BY ASceding.rnk
LIMIT 100;


