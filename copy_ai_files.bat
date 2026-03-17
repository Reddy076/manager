@echo off
set "TARGET_DIR=e:\Deploy\manager\Local_LLM_AI_Features"
if not exist "%TARGET_DIR%" mkdir "%TARGET_DIR%"

if not exist "%TARGET_DIR%\plans" mkdir "%TARGET_DIR%\plans"
copy "e:\Deploy\manager\plans\phased-llm-integration-guide.md" "%TARGET_DIR%\plans\"
copy "e:\Deploy\manager\plans\local-llm-integration-plan.md" "%TARGET_DIR%\plans\"

if not exist "%TARGET_DIR%\src\main\java\com\revature\passwordmanager\service\ai" mkdir "%TARGET_DIR%\src\main\java\com\revature\passwordmanager\service\ai"
xcopy /E /I /Y "e:\Deploy\manager\Rev-PasswordManager (2)\Rev-PasswordManager\src\main\java\com\revature\passwordmanager\service\ai" "%TARGET_DIR%\src\main\java\com\revature\passwordmanager\service\ai"

if exist "e:\Deploy\manager\Rev-PasswordManager (2)\Rev-PasswordManager\src\test\java\com\revature\passwordmanager\service\ai" (
    if not exist "%TARGET_DIR%\src\test\java\com\revature\passwordmanager\service\ai" mkdir "%TARGET_DIR%\src\test\java\com\revature\passwordmanager\service\ai"
    xcopy /E /I /Y "e:\Deploy\manager\Rev-PasswordManager (2)\Rev-PasswordManager\src\test\java\com\revature\passwordmanager\service\ai" "%TARGET_DIR%\src\test\java\com\revature\passwordmanager\service\ai"
)

if not exist "%TARGET_DIR%\src\main\java\com\revature\passwordmanager\controller" mkdir "%TARGET_DIR%\src\main\java\com\revature\passwordmanager\controller"
copy "e:\Deploy\manager\Rev-PasswordManager (2)\Rev-PasswordManager\src\main\java\com\revature\passwordmanager\controller\AIController.java" "%TARGET_DIR%\src\main\java\com\revature\passwordmanager\controller\"

if not exist "%TARGET_DIR%\src\test\java\com\revature\passwordmanager\controller" mkdir "%TARGET_DIR%\src\test\java\com\revature\passwordmanager\controller"
copy "e:\Deploy\manager\Rev-PasswordManager (2)\Rev-PasswordManager\src\test\java\com\revature\passwordmanager\controller\AIControllerTest.java" "%TARGET_DIR%\src\test\java\com\revature\passwordmanager\controller\"

if not exist "%TARGET_DIR%\src\main\java\com\revature\passwordmanager\config" mkdir "%TARGET_DIR%\src\main\java\com\revature\passwordmanager\config"
copy "e:\Deploy\manager\Rev-PasswordManager (2)\Rev-PasswordManager\src\main\java\com\revature\passwordmanager\config\LlmConfig.java" "%TARGET_DIR%\src\main\java\com\revature\passwordmanager\config\"

if not exist "%TARGET_DIR%\src\main\java\com\revature\passwordmanager\exception" mkdir "%TARGET_DIR%\src\main\java\com\revature\passwordmanager\exception"
copy "e:\Deploy\manager\Rev-PasswordManager (2)\Rev-PasswordManager\src\main\java\com\revature\passwordmanager\exception\LlmCommunicationException.java" "%TARGET_DIR%\src\main\java\com\revature\passwordmanager\exception\"

if not exist "%TARGET_DIR%\frontend\src\app\core\api\api" mkdir "%TARGET_DIR%\frontend\src\app\core\api\api"
copy "e:\Deploy\manager\Rev-PasswordManager (2)\Rev-PasswordManager\frontend\src\app\core\api\api\aIPasswordAssistant.service.ts" "%TARGET_DIR%\frontend\src\app\core\api\api\"

if not exist "%TARGET_DIR%\frontend\src\app\core\api\model" mkdir "%TARGET_DIR%\frontend\src\app\core\api\model"
copy "e:\Deploy\manager\Rev-PasswordManager (2)\Rev-PasswordManager\frontend\src\app\core\api\model\aIInsightResponse.ts" "%TARGET_DIR%\frontend\src\app\core\api\model\"

if exist "e:\Deploy\manager\Rev-PasswordManager (2)\Rev-PasswordManager\frontend\src\app\features\ai" (
    if not exist "%TARGET_DIR%\frontend\src\app\features\ai" mkdir "%TARGET_DIR%\frontend\src\app\features\ai"
    xcopy /E /I /Y "e:\Deploy\manager\Rev-PasswordManager (2)\Rev-PasswordManager\frontend\src\app\features\ai" "%TARGET_DIR%\frontend\src\app\features\ai"
)

echo Done
