USE master
GO

DECLARE @mydb nvarchar(50);
-- Ввести имя базы данных
set @mydb = '$(infobase)';
PRINT 'Processing ' + @mydb + ' to online mode...'
EXEC('ALTER DATABASE ' + @mydb + ' SET ONLINE')
PRINT @mydb + ' switched to online mode sucessfully'