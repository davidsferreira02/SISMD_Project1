@echo off
setlocal enabledelayedexpansion

REM Check if implementation parameter is provided
if "%~1"=="" (
    echo ERROR: Please specify the implementation class name.
    echo Usage: %~nx0 [ImplementationClassName]
    echo Example: %~nx0 Sequential
    goto :eof
)

REM Set implementation class name
set "IMPLEMENTATION=%~1"

REM Set relative path from script location
cd /d "%~dp0\.."
set "PROJECT_ROOT=%CD%"
echo Using project root: %PROJECT_ROOT%

REM Set classpath and XML path relative to project root
set "CLASSPATH=%PROJECT_ROOT%\out\production\sismd"
set "XML_PATH=%PROJECT_ROOT%\enwiki.xml"

REM Get current timestamp for log directory and files
for /f "tokens=2 delims==" %%a in ('wmic OS Get localdatetime /value') do set "dt=%%a"
set "timestamp=%dt:~0,8%_%dt:~8,6%"
set "log_dir=%PROJECT_ROOT%\GarbageLogs\%IMPLEMENTATION%\%timestamp%"

REM Create log directory with timestamp
if not exist "%log_dir%" mkdir "%log_dir%"

REM Check if XML file exists
if not exist "%XML_PATH%" (
    echo ERROR: enwiki.xml file not found at %XML_PATH%
    goto :eof
)

REM Detect Java version for GC compatibility
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set "java_version=%%v"
)
set "java_version=%java_version:"=%"
for /f "tokens=1,2 delims=." %%a in ("%java_version%") do (
    set "java_major_version=%%a"
)
echo Detected Java version: %java_version% (Major version: %java_major_version%)

REM ---- DEFAULT GC TESTS (Java's default choice) ----
echo Running default GC tests...
call :run_test "Default_128m_1g" "" "128m" "1024m"


REM ---- SERIAL GC TESTS ----
echo Running Serial GC tests...
call :run_test "Serial_128m_1g" "-XX:+UseSerialGC" "128m" "1024m"
call :run_test "Serial_256m_2g" "-XX:+UseSerialGC" "256m" "2048m"
call :run_test "Serial_512m_4g" "-XX:+UseSerialGC" "512m" "4096m"
call :run_test "Serial_1g_8g" "-XX:+UseSerialGC" "1024m" "8192m"

REM ---- PARALLEL GC TESTS ----
echo Running Parallel GC tests...
call :run_test "Parallel_128m_1g" "-XX:+UseParallelGC" "128m" "1024m"
call :run_test "Parallel_256m_2g" "-XX:+UseParallelGC" "256m" "2048m"
call :run_test "Parallel_512m_4g" "-XX:+UseParallelGC" "512m" "4096m"
call :run_test "Parallel_1g_8g" "-XX:+UseParallelGC" "1024m" "8192m"
call :run_test "Parallel_pause25" "-XX:+UseParallelGC -XX:MaxGCPauseMillis=25" "256m" "2048m"
call :run_test "Parallel_pause50" "-XX:+UseParallelGC -XX:MaxGCPauseMillis=50" "256m" "2048m"
call :run_test "Parallel_pause100" "-XX:+UseParallelGC -XX:MaxGCPauseMillis=100" "256m" "2048m"
call :run_test "Parallel_pause200" "-XX:+UseParallelGC -XX:MaxGCPauseMillis=200" "256m" "2048m"
call :run_test "Parallel_cpu50" "-XX:+UseParallelGC -XX:ParallelGCThreads=4" "256m" "2048m"

REM ---- G1 GC TESTS ----
echo Running G1 GC tests...
call :run_test "G1_128m_1g" "-XX:+UseG1GC" "128m" "1024m"
call :run_test "G1_256m_2g" "-XX:+UseG1GC" "256m" "2048m"
call :run_test "G1_512m_4g" "-XX:+UseG1GC" "512m" "4096m"
call :run_test "G1_1g_8g" "-XX:+UseG1GC" "1024m" "8192m"
call :run_test "G1_pause10" "-XX:+UseG1GC -XX:MaxGCPauseMillis=10" "256m" "2048m"
call :run_test "G1_pause25" "-XX:+UseG1GC -XX:MaxGCPauseMillis=25" "256m" "2048m"
call :run_test "G1_pause50" "-XX:+UseG1GC -XX:MaxGCPauseMillis=50" "256m" "2048m"
call :run_test "G1_pause100" "-XX:+UseG1GC -XX:MaxGCPauseMillis=100" "256m" "2048m"
call :run_test "G1_pause200" "-XX:+UseG1GC -XX:MaxGCPauseMillis=200" "256m" "2048m"
call :run_test "G1_regions" "-XX:+UseG1GC -XX:G1HeapRegionSize=2m" "256m" "2048m"
call :run_test "G1_conc2" "-XX:+UseG1GC -XX:ConcGCThreads=2" "256m" "2048m"

REM ---- ZGC TESTS  ----
echo Running ZGC tests...
call :run_test "ZGC_128m_1g" "-XX:+UseZGC" "128m" "1024m"
call :run_test "ZGC_256m_2g" "-XX:+UseZGC" "256m" "2048m"
call :run_test "ZGC_512m_4g" "-XX:+UseZGC" "512m" "4096m"
call :run_test "ZGC_1g_8g" "-XX:+UseZGC" "1024m" "8192m"






echo All tests completed. Results stored in: %log_dir%

goto :eof

REM ---- Test execution function ----
:run_test
REM Parameters: %1=test_name, %2=gc_options, %3=initial_heap, %4=max_heap
set "test_name=%~1"
set "gc_options=%~2"
set "initial_heap=%~3"
set "max_heap=%~4"

echo Running test: %test_name%

REM Execute the test with specified options
java -cp "%CLASSPATH%" ^
     %gc_options% ^
     -Xms%initial_heap% -Xmx%max_heap% ^
     -verbose:gc ^
     -Xlog:gc*:file="%log_dir%\%test_name%_gc.log" ^
     %IMPLEMENTATION% "%XML_PATH%" > "%log_dir%\%test_name%_output.txt" 2>&1

if %ERRORLEVEL% NEQ 0 (
    echo WARNING: Test %test_name% exited with error code %ERRORLEVEL%
    echo Check log file: "%log_dir%\%test_name%_output.txt"
) else (
    echo Test %test_name% completed successfully
)

REM Add a small delay between tests
timeout /t 2 > nul

exit /b 0