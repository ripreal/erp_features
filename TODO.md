# erp_features

TODO: 
- Установитть дженкинс через инсталлятор и првоеритт дефолтные опции
- Какой адрес дженкинса по дефолту
- Добавить русские синонимы при навигации в дженкинсе
- проверить команду opm install vanessa-runner
- добавить ссылки на все  инсталляторы
- уточнить что такое порт 1540 для 1с
- заполнить прмер форматов расписания

# Подготовка
oneScript
плагин для vsCode для отладки jenkinsfile
runner
irac

# Предпоссылки
* Имя администраторов во всех базах одинаковые
* Имя базы данныйх в sql сервере и сервере 1с предприятия совпадает
* Зарегистрирована компонента V83.ComConnector
* Платформа не ниже 8.3.12
* нужно сконфигурировать использование shared-libraries https://blog.ippon.tech/setting-up-a-shared-library-and-seed-job-in-jenkins-part-2/
* нужно установить аллюр плагин и сконфигурировать его http://localhost:8991/configureTools/ 
https://github.com/allure-framework/allure-docs/blob/master/docs/reporting/jenkins.adoc
* Нужно установить кодировку UTF-8 https://medium.com/pacroy/how-to-fix-jenkins-console-log-encoding-issue-on-windows-a1f4b26e0db4
* Разрешить метод createTempFile() Scripts not permitted to use staticMethod java.io.File createTempFile java.lang.String java.lang.String. Administrators can decide whether to approve or reject this signature.


# Рекомендации
* рекомендуемые тулзы: janjoerke.jenkins-pipeline-linter-connector-1.1.7  alexkrechik.cucumberautocomplete-2.14.0 xdrivendevelopment.language-1c-bsl-1.12.0


Инфа по оформлению статьи
https://habr.com/ru/company/true_engineering/blog/447812/