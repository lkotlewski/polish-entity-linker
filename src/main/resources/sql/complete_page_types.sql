--founds special categories with count of intances
select * from (select instance_of_id, count(*) from wiki_item_instance_of
               group by instance_of_id order by count(*) desc) mp
                  left join wiki_item on mp.instance_of_id = wiki_item.id where label_eng like 'Wikimedia%';

--uses informations from previous select
update wiki_item
set page_type = 3
where id in
      (select wiki_item_id from wiki_item_instance_of where instance_of_id = 'Q4167410');

update wiki_item
set page_type = 1
where id in
      (select wiki_item_id from wiki_item_instance_of where instance_of_id = 'Q4167836');

update wiki_item
set page_type = 4
where id in
      (select wiki_item_id from wiki_item_instance_of where instance_of_id = 'Q11266439');

update wiki_item
set page_type = 5
where id in
      (select wiki_item_id
       from wiki_item_instance_of
       where instance_of_id in
             ('Q14204246', 'Q13406463', 'Q11753321', 'Q36330215', 'Q4663903', 'Q20010800', 'Q18340550', 'Q15184295',
              'Q17362920', 'Q17146139', 'Q14827288', 'Q17379835', 'Q21286738', 'Q15138389', 'Q35252665', 'Q54913642'));
update wiki_item
set page_type = 0
where page_type isnull
