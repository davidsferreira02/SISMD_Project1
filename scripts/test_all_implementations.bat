@echo off
setlocal

REM Navigate to scripts directory (where this script and run_gc_tests.bat are located)
cd /d "%~dp0"

echo ======================================================
echo Testing all implementations with different GC options
echo ======================================================
echo.

echo [1/5] Testing Sequential implementation...
call run_gc_tests.bat Sequential
echo.

echo [2/5] Testing Multithreaded implementation...
call run_gc_tests.bat Multithreaded
echo.

echo [3/5] Testing MultithreadedThreadPools implementation...
call run_gc_tests.bat MultithreadedThreadPools
echo.

echo [4/5] Testing ForkJoinSolution implementation...
call run_gc_tests.bat ForkJoinSolution
echo.

echo [5/5] Testing CompletableFutureSolution implementation...
call run_gc_tests.bat CompletableFutureSolution
echo.

echo ======================================================
echo All tests completed! Results are available in:
echo %~dp0..\GarbageLogs\
echo ======================================================

endlocal