@echo off
setlocal enabledelayedexpansion

REM Set output CSV file
set "OUTPUT_CSV=benchmark_results.csv"
echo Implementation,Threads,FileSize,RunNumber,ElapsedTime,MemoryUsage,CPUTime > %OUTPUT_CSV%

REM Define parameters to test
set "PARALLEL_IMPLEMENTATIONS=Multithreaded MultithreadedThreadPools ForkJoinSolution CompletableFutureSolution"
set "SEQUENTIAL_IMPLEMENTATION=Sequential"
set "THREAD_COUNTS=50 100 500"
set "FILE_SIZES=10000 50000 100000"
set "REPEAT_COUNT=3"

REM Set classpath
set "CLASSPATH=%~dp0..\out\production\sismd"

REM Run Sequential Implementation
set "SEQ_IMPL=%SEQUENTIAL_IMPLEMENTATION%"
echo Running %SEQ_IMPL%
for %%f in (%FILE_SIZES%) do (
    for /L %%r in (1,1,%REPEAT_COUNT%) do (
        echo Running %SEQ_IMPL% with %%f pages, run %%r
        
        REM Run the implementation and capture output
        set "OUTPUT_FILE=%TEMP%\%SEQ_IMPL%_1_%%f_%%r_output.txt"
        
        java -cp %CLASSPATH% %SEQ_IMPL% --pages %%f > !OUTPUT_FILE!
        
        REM Extract metrics from output
        set "elapsed="
        set "memory="
        set "cpu="

        for /f "tokens=1,* delims=:" %%a in ('findstr /B /C:"Elapsed time:" !OUTPUT_FILE!') do (
            set "value_part=%%b"
            set "elapsed=!value_part:ms=!"
            set "elapsed=!elapsed: =!" 
        )
        for /f "tokens=1,* delims=:" %%a in ('findstr /B /C:"Usage Memory:" !OUTPUT_FILE!') do (
            set "value_part=%%b"
            set "memory=!value_part: bytes=!" 
            set "memory=!memory:bytes=!"   
            set "memory=!memory: =!"    
        )
        REM For CPU Time, the format is "Usage Cpu Time <number> seconds" (no colon after "Time")
        REM Default delimiters for FOR /F are space and tab.
        REM tokens=4 should get the number directly if output is "Usage Cpu Time <num> seconds"
        for /f "tokens=4" %%c in ('findstr /B /C:"Usage Cpu Time" !OUTPUT_FILE!') do (
            set "cpu=%%c"
            REM cpu might still have "seconds" if not perfectly tokenized, or be just the number.
            REM The following lines are defensive.
            set "cpu=!cpu:seconds=!" 
            set "cpu=!cpu: =!" 
        )
        REM Alternative for CPU if tokens=4 is not robust enough:
        REM for /f "tokens=3,*" %%c in ('findstr /B /C:"Usage Cpu Time" !OUTPUT_FILE!') do (
        REM     set "temp_cpu_val=%%d"  REM This should be "<number> seconds"
        REM     set "cpu=!temp_cpu_val: seconds=!"
        REM     set "cpu=!cpu:seconds=!" 
        REM     set "cpu=!cpu: =!"      
        REM )

        REM If any value is missing, set to 0
        if "!elapsed!"=="" set "elapsed=0"
        if "!memory!"=="" set "memory=0"
        if "!cpu!"=="" set "cpu=0"
        
        REM Append results to CSV (using 1 for thread count)
        echo %SEQ_IMPL%,1,%%f,%%r,!elapsed!,!memory!,!cpu! >> %OUTPUT_CSV%
    )
)

REM Run Parallel Implementations
for %%i in (%PARALLEL_IMPLEMENTATIONS%) do (
    for %%t in (%THREAD_COUNTS%) do (
        for %%f in (%FILE_SIZES%) do (
            for /L %%r in (1,1,%REPEAT_COUNT%) do (
                echo Running %%i with %%t threads, %%f pages, run %%r
                
                REM Run the implementation and capture output
                set "OUTPUT_FILE=%TEMP%\%%i_%%t_%%f_%%r_output.txt"
                
                java -cp %CLASSPATH% %%i --threads %%t --pages %%f > !OUTPUT_FILE!
                
                REM Extract metrics from output
                set "elapsed="
                set "memory="
                set "cpu="

                for /f "tokens=1,* delims=:" %%a in ('findstr /B /C:"Elapsed time:" !OUTPUT_FILE!') do (
                    set "value_part=%%b"
                    set "elapsed=!value_part:ms=!"
                    set "elapsed=!elapsed: =!" 
                )
                for /f "tokens=1,* delims=:" %%a in ('findstr /B /C:"Usage Memory:" !OUTPUT_FILE!') do (
                    set "value_part=%%b"
                    set "memory=!value_part: bytes=!" 
                    set "memory=!memory:bytes=!"   
                    set "memory=!memory: =!"    
                )
                REM For CPU Time, the format is "Usage Cpu Time <number> seconds" (no colon after "Time")
                REM Default delimiters for FOR /F are space and tab.
                REM tokens=4 should get the number directly if output is "Usage Cpu Time <num> seconds"
                for /f "tokens=4" %%c in ('findstr /B /C:"Usage Cpu Time" !OUTPUT_FILE!') do (
                    set "cpu=%%c"
                    REM cpu might still have "seconds" if not perfectly tokenized, or be just the number.
                    REM The following lines are defensive.
                    set "cpu=!cpu:seconds=!" 
                    set "cpu=!cpu: =!" 
                )
                REM Alternative for CPU if tokens=4 is not robust enough:
                REM for /f "tokens=3,*" %%c in ('findstr /B /C:"Usage Cpu Time" !OUTPUT_FILE!') do (
                REM     set "temp_cpu_val=%%d"  REM This should be "<number> seconds"
                REM     set "cpu=!temp_cpu_val: seconds=!"
                REM     set "cpu=!cpu:seconds=!" 
                REM     set "cpu=!cpu: =!"      
                REM )
                
                REM If any value is missing, set to 0
                if "!elapsed!"=="" set "elapsed=0"
                if "!memory!"=="" set "memory=0"
                if "!cpu!"=="" set "cpu=0"
                
                REM Append results to CSV
                echo %%i,%%t,%%f,%%r,!elapsed!,!memory!,!cpu! >> %OUTPUT_CSV%
            )
        )
    )
)

echo All tests completed. Results saved to %OUTPUT_CSV%
