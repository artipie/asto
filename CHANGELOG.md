# v1.13.0
- 5776ac19 - feat: asto as multiple modules project (#444)

# v1.12.3
 - c72d5e2 - feat: 'StorageValuePipeline' should process large-size content without buffering it all in memory (#433)
 - 93846af - fix: readme corrections (#435)
 - d5962d8 - fix: extended README.md (#434)

# v1.12.2
 - 11b03beb fix: `StorageValuePipeline` implemented without reactive IO

# v1.12.1
 - d65bc72e build(deps): bump jetcd-core from 0.5.11 to 0.7.1
 - 92307e48 fix: added parts information to `CompleteMultipartUploadRequest`

# v1.12.0

 - 31454e8 - fix: Wrong implementation of the S3Storage
 - 066ad5f - fix: fixed BucketTest#shouldUploadPartAndCompleteMultipartUpload test
 - c0b1d43 test: fix tests for `LoggingStorage`
 - 6fd5a7f fix: do not use deprecated methods in S3Storage
 - ac9e4ea Migrate to ppom 1.1
 - 28915dc deps: bump s3mock from 2.1.19 to 2.4.8 (#404) 
 - e4adcb2 feat: introduce helper for Meta's standard methods (#394) 
 - 027eb65 fix(storage): add metadata method to standard decorator (#395) 
 - a98e79d test: cover BlockingStorage and RxStorageWrapper (#386)

# v1.11.0

 - a0e4296 - fix(fs): check path for FS read (#385)
   by Kirill <g4s8.public@gmail.com>
 - dbbdbe5 - test: setup etcd cluster for StorageExtension (#377)
   by Olivier B. OURA <baudoliver7@gmail.com>
 - 1435113 - fix: fix CI to run tests of EtcdStorageITCase (#380)
   by Olivier Baudouin OURA <baudoliver7@gmail.com>
 - 0ba492e - test: add tests for BlockingStorage (#378)
   by Denis Garus <garus.d.g@gmail.com>
 - 324a0b3 - fix: prevent file to be written out of FileStorage scope (#379)
   by Olivier Baudouin OURA <baudoliver7@gmail.com>
 - 96606d1 - fix: fix delete all operation (#382)
   by Alexander <38591972+genryxy@users.noreply.github.com>

# v1.10.0

  - e3a281b9 feat: introduce a default method in Storage to delete prefixed keys (#368)
  - efc54e69 test: add more tests for SubStorage (#375)
  - 4d52959d test: LoggingStorage tests (#374)
  - b2a64be6 deps: bump vertx.version from 4.1.1 to 4.2.1 (#356)
  - bc45a44b fix: substorage compare keys by string representation (#373)
  - 089ee1e6 feat: add storage Key operations (#365)
  - 1014d517 test: add tests for logging (#367)
  - aee483fc test: add  tests for SubStorage (#366)
  - aa0493ef deps: bump jcabi-log from 0.18.1 to 0.20.1 (#357)
  - 047aba87 deps: bump resilience4j-retry from 1.5.0 to 1.7.1 (#361)
  - 5d64f1be deps: bump jetcd-core from 0.5.4 to 0.5.11 (#358)
  - 105483b2 deps: bump jetcd-test from 0.5.4 to 0.5.11 (#360)
  - 7eaa8ce3 cI: removed gitlinter
  - c7aea333 feat(metadata): storage metadata API (#350)
  - fe8ba924 ci: added dependabot 

# v1.9.0

  - 90387fff fix: `SubStorage` list operation (#353)
  - 60c0d4c7 feat: excludes any part from storage key (#347)
  - 7a6a1309 feat: extended `StorageValuePipeline` to accept diff keys (#349)
  - 7e07dd52 test: Adds base tests for `LoggingStorage` (#344)

# v1.8.0

  - c85259 feat: adds UncheckedSupplier (#342)
  - e35da32 ci: artipie central snapshots config

# v1.7.0

  - ce5987f feat: test resource save to file (#339)

# v1.6.0

 - feat: Extended `StorageValuePipeline` to return processing result (#333)

# v1.5.1

 - fix: Make method `StorageValuePipeline.process()` public (#333)

# v1.5.0

 - feat: Added class to process asto value as input/output streams (#334)
 
# v1.4.0

 - feat: process storage item as input stream (#334)

# v1.3.3

 - deps: declare optional dependencies as provided (#331)

# 1.3.2

 - dep: exclude fasterxml jackson databind from vertx-rx-java2

# 1.3.1

 - dep: exclude android annotations from etcd

# 1.3.0

 - ci: added release script
 - feature: copy storage with predicate
 - deps: updated vertx, excluded jackson-databind
 - deps: updated vertx, checked all deps (see issue descr) and excluded jackson-databind from main code.

## 1.2.0

### Benchmark storage #314
 - feature: add implementation for `list` and `size`, optimize `delete`
 - feature: add implementation for delete and exists
 - feature: add implementation of  `save` and `value` for benchmark storage

### Documentation
 - doc: added links to javadocs
