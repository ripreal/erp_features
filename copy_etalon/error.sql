use master
DECLARE @mydb nvarchar(50);
set @mydb = '$(restoreddb)';
if db_id(@mydb) is null
begin
RAISERROR ('Error, database is not exist', 11, 1)
end