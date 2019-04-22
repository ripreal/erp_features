USE master
GO

DECLARE @mydb nvarchar(50);
-- Ввести имя базы данных
set @mydb = '$(infobase)';

PRINT 'Removing ' + @mydb + ' from database...'

EXEC('ALTER DATABASE ' + @mydb + ' SET SINGLE_USER WITH ROLLBACK IMMEDIATE')

EXEC ('DROP DATABASE ' + @mydb)

PRINT @mydb + ' removed sucessfully'