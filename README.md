# incremental-compiler
A compiler of java code which supports incremental build. Internally it stores metadata information in target directory so next time we run the build we only rebuild files which has been changed.

## How to build?

```
./gradlew assemble
```
A ZIP file with application will be stored in ./build/distributions

## How to use?

```
./bin/incremental-compiler --source-files-dir {SOURCE_FILES_DIR} --class-files-dir {CLASS_FILES_DIR}
```

See help:
```aidl
./bin/incremental-compiler --help

Usage: Incremental java compiler. It does recompilation only for files which has been changed.
           [OPTIONS]

Options:
  --source-files-dir PATH  Directory with java files
  --class-files-dir PATH   Directory to store compiled java files ( .class
                           files )
  -h, --help               Show this message and exit

```

See an example of compilation improvement for the second build 15s vs 3s:

```
[~/Private/incremental-compiler/build/distributions]$ time ./incremental-compiler-1.0-SNAPSHOT/bin/incremental-compiler --source-files-dir /Users/atcarevs/Private/Compilation_test --class-files-dir /tmp/compilation
Current working directory : /Users/atcarevs/Private/incremental-compiler/build/distributions
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
Compiling java files 100% │██████████████████████████████████████████████████████████████████████████│ 11/11 (0:00:08 / 0:00:00) Compiling test/SecondClass6.java
Compiling depending java files  ? % ││ 0/0 (0:00:00 / ?)
./incremental-compiler-1.0-SNAPSHOT/bin/incremental-compiler      15.54s user 1.59s system 164% cpu 10.410 total
[~/Private/incremental-compiler/build/distributions]$ time ./incremental-compiler-1.0-SNAPSHOT/bin/incremental-compiler --source-files-dir /Users/atcarevs/Private/Compilation_test --class-files-dir /tmp/compilation
Current working directory : /Users/atcarevs/Private/incremental-compiler/build/distributions
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
Cleaning old .class files 100% │█████████████████████████████████████████████████████████████████████████████████████████████████████│ 11/11 (0:00:00 / 0:00:00)
Compiling java files 100% │██████████████████████████████████████████████████████████████████████████████████████████████████████████│ 11/11 (0:00:00 / 0:00:00)
Compiling depending java files  ? % ││ 0/0 (0:00:00 / ?)
./incremental-compiler-1.0-SNAPSHOT/bin/incremental-compiler      3.11s user 0.35s system 144% cpu 2.390 total
[~/Private/incremental-compiler/build/distributions]$
```
