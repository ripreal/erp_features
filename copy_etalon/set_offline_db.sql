USE master
GO

DECLARE @mydb nvarchar(50);
-- Ввести имя базы данных
set @mydb = '$(infobase)';
PRINT 'Processing ' + @mydb + ' to offline mode...'
EXEC('ALTER DATABASE ' + @mydb + ' SET OFFLINE WITH ROLLBACK IMMEDIATE')
PRINT @mydb + ' switched to offline mode sucessfully'