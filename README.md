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
