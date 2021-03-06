//
//  STSyncModule.m
//  StorjMobile
//
//  Created by Bogdan Artemenko on 3/28/18.
//  Copyright © 2018 Storj. All rights reserved.
//

#import "STSyncModule.h"
#import "STSettingsRepository.h"
#import "STSyncService.h"
#import "STSyncScheduler.h"
#import "SyncSettings.h"

#define RESOLVER "RCTresolver"
#define REJECTER "RCTrejecter"

@implementation STSyncModule

RCT_EXPORT_MODULE(SyncModuleIOS);

@synthesize _database;
@synthesize _bucketRepository, _fileRepository, _uploadFileRepository, _syncQueueRepository;

static STSettingsRepository *settingsRepository;

-(FMDatabase *) database{
  if(!_database){
    _database = [[STDatabaseFactory getSharedDatabaseFactory] getSharedDb];
  }
  return _database;
}

-(STBucketRepository *)bucketRepository{
  if(!_bucketRepository){
    _bucketRepository = [[STBucketRepository alloc] init];
  }
  return _bucketRepository;
}

-(STFileRepository *) fileRepository{
  if(!_fileRepository){
    _fileRepository = [[STFileRepository alloc] init];
  }
  return _fileRepository;
}

-(STUploadFileRepository *) uploadFileRepository{
  if(!_uploadFileRepository){
    _uploadFileRepository = [[STUploadFileRepository alloc] init];
  }
  return _uploadFileRepository;
}

-(SyncQueueRepository *) syncQueueRepository
{
  if(!_syncQueueRepository)
  {
    _syncQueueRepository = [[SyncQueueRepository alloc] init];
  }
  return _syncQueueRepository;
}

-(STSettingsRepository *) settingsRepository
{
  if(!settingsRepository)
  {
    settingsRepository = [[STSettingsRepository alloc] init];
  }
  return settingsRepository;
}

RCT_REMAP_METHOD(listBuckets,
                 listBucketsWithSortingMode: (NSString *) sortingMode
                 withResolver: (RCTPromiseResolveBlock) resolver
                 andRejecter: (RCTPromiseRejectBlock) rejecter){
  NSLog(@"SyncModule: listBuckets");
  [MethodHandler
   invokeParallelWithParams:@{@RESOLVER: resolver,
                               @REJECTER : rejecter}
   andMethodHandlerBlock:^(NSDictionary * _Nonnull param) {
     
     NSArray *bucketDbos = [NSArray arrayWithArray:
                            [sortingMode isEqualToString:@"name"]
                              ? [[self bucketRepository] getAllWithOrderByColumn:sortingMode order:YES]
                              : [[self bucketRepository] getAll]];
     
     int length = bucketDbos.count;
     NSMutableArray<STBucketModel *> * bucketModels = [NSMutableArray arrayWithCapacity:length];
     for (int i = 0; i < length; i++){
       bucketModels[i] = [bucketDbos[i] toModel];
     }
     SingleResponse *response = [SingleResponse
                                 successSingleResponseWithResult:
                                 [DictionaryUtils convertToJsonWithArray:bucketModels]];
     RCTPromiseResolveBlock resolve = param[@RESOLVER];
     resolve([response toDictionary]);
   }];
}

RCT_REMAP_METHOD(listFiles,
                 listFilesFromBucket: (NSString *) bucketId
                 withSortingMode: (NSString *) sortingMode
                 withResolver: (RCTPromiseResolveBlock) resolver
                 andRejecter: (RCTPromiseRejectBlock) rejecter){
  [MethodHandler
   invokeParallelWithParams:@{@RESOLVER: resolver,
                               @REJECTER : rejecter}
   andMethodHandlerBlock:^(NSDictionary * _Nonnull param) {
     
     NSArray <FileDbo *> * fileDbos = [sortingMode isEqualToString:@"name"]
     ? [NSArray arrayWithArray: [[self fileRepository] getAllFromBucket: bucketId
                                                          orderByColumn: sortingMode
                                                             descending: YES]]
     : [NSArray arrayWithArray:[[self fileRepository] getAllFromBucket:bucketId]];
     
     int length = fileDbos.count;
     NSMutableArray <STFileModel *> * fileModels = [NSMutableArray arrayWithCapacity:length];
     for(int i = 0; i < length; i++){
       fileModels[i] = [[STFileModel alloc] initWithFileDbo:fileDbos[i]];
     }

     SingleResponse *response = [SingleResponse
                                 successSingleResponseWithResult:
                                 [DictionaryUtils convertToJsonWithArray:fileModels]];
     RCTPromiseResolveBlock resolve = param[@RESOLVER];
     resolve([response toDictionary]);
   }];
}

RCT_REMAP_METHOD(listAllFiles,
                 listAllFilesBySortingMode: (NSString *) sortingMode
                 withResolver: (RCTPromiseResolveBlock) resolver
                 andRejecter: (RCTPromiseRejectBlock) rejecter) {
  [MethodHandler
   invokeParallelWithParams:@{@RESOLVER: resolver,
                               @REJECTER : rejecter}
   andMethodHandlerBlock:^(NSDictionary * _Nonnull param) {
     NSArray<FileDbo *> *fileDbos = [sortingMode isEqualToString:@"name"]
     ? [[self fileRepository] getAllWithOrderByColumn:sortingMode order:YES]
     : [[self fileRepository] getAll];
     int length = fileDbos.count;
     NSMutableArray *fileModels = [NSMutableArray arrayWithCapacity: length];
     for(int i = 0; i < length; i++){
       fileModels[i] = [[STFileModel alloc] initWithFileDbo:fileDbos[i]];
     }
     RCTPromiseResolveBlock resolve = param[@RESOLVER];
     resolve([[SingleResponse successSingleResponseWithResult:
               [DictionaryUtils convertToJsonWithArray:fileModels]]toDictionary] );
   }];
}

RCT_REMAP_METHOD(listUploadingFiles,
                 listUploadingFilesWithBucketId: (NSString *) bucketId
                 withResolver: (RCTPromiseResolveBlock) resolver
                 andRejecter: (RCTPromiseRejectBlock) rejecter){
  [MethodHandler
   invokeParallelWithParams:@{@RESOLVER: resolver,
                               @REJECTER : rejecter}
   andMethodHandlerBlock:^(NSDictionary * _Nonnull param) {
     NSArray <UploadFileDbo *> *ufileDbos = [NSArray arrayWithArray:[[self uploadFileRepository]
                                                                     getAll]];
     int length = ufileDbos.count;
     NSMutableArray <STUploadFileModel *> *fileModels = [NSMutableArray arrayWithCapacity:length];
     for (int i = 0; i < length; i++) {
       fileModels[i] = [[STUploadFileModel alloc] initWithUploadFileDbo:ufileDbos[i]];
     }
     
     NSDictionary *(^callback)(NSObject *) = ^(NSObject *object)
     {
       NSDictionary *dict = nil;
       
       dict = [(STUploadFileModel *)object toDictionaryProgress];
       
       return dict;
     };
     
     SingleResponse *response = [SingleResponse successSingleResponseWithResult:
                                 [DictionaryUtils convertToJsonWithArray: fileModels
                                                      andConvertCallback: callback]];
     RCTPromiseResolveBlock resolve = param[@RESOLVER];
     resolve([response toDictionary]);
   }];
}

RCT_REMAP_METHOD(getSyncQueue, getSyncQueueWithResolver: (RCTPromiseResolveBlock) resolver
                                            andRejecter: (RCTPromiseRejectBlock) rejecter)
{
  void (^callback)(NSDictionary *) = ^(NSDictionary *param)
  {
    NSArray <STSyncQueueEntryModel *> *syncQueue = [NSArray arrayWithArray:
                                                  [[self syncQueueRepository] getAll]];
    
    SingleResponse *response = [SingleResponse successSingleResponseWithResult:
                                [DictionaryUtils convertToJsonWithArray: syncQueue]];
    
    
    RCTPromiseResolveBlock resolve = param[@RESOLVER];
    resolve([response toDictionary]);
  };
  
  [MethodHandler invokeParallelWithParams: @{@RESOLVER: resolver, @REJECTER: rejecter}
                    andMethodHandlerBlock: callback];
}

RCT_REMAP_METHOD(getSyncQueueEntry, getSyncQueueEntryWithId: (int) _id
                                                   Resolver: (RCTPromiseResolveBlock) resolver
                                                andRejecter: (RCTPromiseRejectBlock) rejecter)
{
  void (^callback)(NSDictionary *) = ^(NSDictionary *param)
  {
    RCTPromiseResolveBlock resolve = param[@RESOLVER];
    STSyncQueueEntryModel *entry = [[self syncQueueRepository] getById: _id];
    
    if(!entry)
    {
      resolve([[SingleResponse errorResponseWithMessage:@"Can't find entry"] toDictionary]);
      return;
    }
    
    SingleResponse *response = [SingleResponse successSingleResponseWithResult:
                                [DictionaryUtils convertToJsonWithDictionary:
                                 [entry toDictionary]]];
    resolve([response toDictionary]);
  };
  
  [MethodHandler invokeParallelWithParams: @{@RESOLVER: resolver, @REJECTER: rejecter}
                    andMethodHandlerBlock: callback];
}

RCT_REMAP_METHOD(updateSyncQueueEntryStatus, updateSyncQueueEntryStatusWithId: (int) _id
                                                                    newStatus: (int) newStatus
                                                                     Resolver: (RCTPromiseResolveBlock) resolver
                                                                  andRejecter: (RCTPromiseRejectBlock) rejecter)
{
  if(newStatus != 3 && newStatus != 0)
  {
    resolver([[SingleResponse errorResponseWithMessage: @"Can set to idle or cancelled state only"] toDictionary]);
    return;
  }
  
  UpdateDboCallback callback = ^(SyncQueueEntryDbo *dbo)
  {
    dbo.status = newStatus;
  };
  
  [self updateSyncQueueEntryWithId: _id
                          callback: callback
                          Resolver: resolver
                       andRejecter: rejecter];
}

RCT_REMAP_METHOD(updateSyncQueueEntryFileName, updateSyncQueueEntryFileNameWith: (int) _id
                                                                       newFileName: (NSString *) newFileName
                                                                        Resolver: (RCTPromiseResolveBlock) resolver
                                                                     andRejecter: (RCTPromiseRejectBlock) rejecter)
{
  if(!newFileName)
  {
    resolver([[SingleResponse errorResponseWithMessage: @"No file name provided!"] toDictionary]);
  }
  
  UpdateDboCallback callback = ^(SyncQueueEntryDbo *dbo)
  {
    dbo.fileName = newFileName;
    dbo.status = 0;
  };
  
  [self updateSyncQueueEntryWithId: _id
                          callback: callback
                          Resolver: resolver
                       andRejecter: rejecter];
}

typedef void(^UpdateDboCallback)(SyncQueueEntryDbo *);

-(void) updateSyncQueueEntryWithId: (int) _id
                                callback: (UpdateDboCallback) updateCallback
                                Resolver: (RCTPromiseResolveBlock) resolver
                             andRejecter: (RCTPromiseRejectBlock) rejecter
{
  void (^callback)(NSDictionary *) = ^(NSDictionary *param)
  {
    RCTPromiseResolveBlock resolve = param[@RESOLVER];
    
    STSyncQueueEntryModel *entry = [[self syncQueueRepository] getById: _id];
    
    if(!entry)
    {
      resolve([[SingleResponse errorResponseWithMessage:@"Can't find entry"] toDictionary]);
      return;
    }
    
    if(entry.status == 1 || entry.status == 5)
    {
      resolve([[SingleResponse errorResponseWithMessage:@"Can't update entry that is beeing processed"] toDictionary]);
      return;
    }
    
    SyncQueueEntryDbo *dbo = [entry toDbo];
    updateCallback(dbo);
    
    entry = [[STSyncQueueEntryModel alloc] initWithDbo: dbo];
    
    Response *updateResponse = [[self syncQueueRepository] updateWithModel: entry];
    
    if(![updateResponse isSuccess])
    {
      resolve([updateResponse toDictionary]);
      return;
    }
    
    SingleResponse *response = [SingleResponse successSingleResponseWithResult:
                                [DictionaryUtils convertToJsonWithDictionary:
                                 [entry toDictionary]]];
    resolve([response toDictionary]);
  };
  
  [MethodHandler invokeParallelWithParams: @{@RESOLVER: resolver, @REJECTER: rejecter}
                    andMethodHandlerBlock: callback];
}


RCT_REMAP_METHOD(getUploadingFile, getUploadingFileWithFileHandle: (NSString *) fileHandle
                 WithResolver: (RCTPromiseResolveBlock) resolver
                 andRejecter: (RCTPromiseRejectBlock) rejecter){
  [MethodHandler
   invokeParallelWithParams:@{@RESOLVER: resolver,
                               @REJECTER : rejecter}
   andMethodHandlerBlock:^(NSDictionary * _Nonnull param) {
     RCTPromiseResolveBlock resolve = param[@RESOLVER];
     
     if(!fileHandle){
       SingleResponse *response = [SingleResponse errorResponseWithMessage:@"invalid file handle"];
       resolve([response toDictionary]);
       return;
     }
     STUploadFileModel *uploadingFileModel = [[self uploadFileRepository] getByFileId:fileHandle];
     SingleResponse *response;
     
     if(!uploadingFileModel){
       response = [SingleResponse errorResponseWithMessage:@"Uploading file not found"];
     } else {
       response = [SingleResponse successSingleResponseWithResult:
                   [DictionaryUtils convertToJsonWithDictionary:
                    [uploadingFileModel toDictionaryProgress]]];
     }
     resolve([response toDictionary]);
   }];
}

RCT_REMAP_METHOD(getFile,
                 getFileWithFileId: (NSString *) fileId
                 WithResolver: (RCTPromiseResolveBlock) resolver
                 andRejecter: (RCTPromiseRejectBlock) rejecter){
  [MethodHandler
   invokeParallelWithParams:@{@RESOLVER: resolver,
                               @REJECTER : rejecter}
   andMethodHandlerBlock:^(NSDictionary * _Nonnull param) {
     RCTPromiseResolveBlock resolve = param[@RESOLVER];
     if(!fileId){
       SingleResponse *errorResponse = [SingleResponse errorResponseWithMessage:@"Invalid fileId"];
       resolve([errorResponse toDictionary]);
       return;
     }
     FileDbo *fileDbo = [[self fileRepository] getByFileId:fileId];
     SingleResponse *response;
     if(!fileDbo){
       response = [SingleResponse errorResponseWithMessage:@"File Not Found"];
     } else {
       STFileModel *fileModel = [[STFileModel alloc] initWithFileDbo:fileDbo];
       NSDictionary *fileModelDict = [fileModel toDictionary];
       response = [SingleResponse successSingleResponseWithResult:[DictionaryUtils
                                                                   convertToJsonWithDictionary:
                                                                   fileModelDict]];
     }
     resolve([response toDictionary]);
   }];
}

RCT_REMAP_METHOD(updateBucketStarred,
                 updateBucketStarredWithBucketId: (NSString *) bucketId
                 starredFlag:(BOOL) isStarred
                 WithResolver: (RCTPromiseResolveBlock) resolver
                 andRejecter: (RCTPromiseRejectBlock) rejecter){
  [MethodHandler
   invokeParallelWithParams:@{@RESOLVER: resolver,
                               @REJECTER : rejecter}
   andMethodHandlerBlock:^(NSDictionary * _Nonnull param) {
     RCTPromiseResolveBlock resolve = param[@RESOLVER];
     resolve([[[self bucketRepository] updateById:bucketId starred:isStarred] toDictionary]);
   }];
}

RCT_REMAP_METHOD(updateFileStarred,
                 updateFileStarredWithBucketId: (NSString *) fileId
                 starredFlag:(BOOL) isStarred
                 WithResolver: (RCTPromiseResolveBlock) resolver
                 andRejecter: (RCTPromiseRejectBlock) rejecter){
  [MethodHandler
   invokeParallelWithParams:@{@RESOLVER: resolver,
                               @REJECTER : rejecter}
   andMethodHandlerBlock:^(NSDictionary * _Nonnull param) {
     RCTPromiseResolveBlock resolve = param[@RESOLVER];
     resolve([[[self fileRepository] updateById:fileId starred:isStarred] toDictionary]);
   }];
}

RCT_REMAP_METHOD(checkFile,
                 checkFileWithFileId: (NSString *) fileID localPath: (NSString *) localPath resolver: (RCTPromiseResolveBlock) resolver
                 andRejecter: (RCTPromiseRejectBlock) rejecter){
  NSLog(@"Checking file: ID: %@, localPath: %@", fileID, localPath);
  if(!localPath) {
    resolver([[Response errorResponseWithMessage:@"Error: Path is null"] toDictionary]);
    return;
  }
  
  BOOL isDirectory;
  BOOL isFileExists = [[NSFileManager defaultManager] fileExistsAtPath:localPath
                                                           isDirectory:&isDirectory];
  NSLog(@"Is file Exists: %d, isDir: %d", isFileExists, isDirectory);
  if(!isFileExists || isDirectory) {
    Response *updateResponse = [[self _fileRepository] updateById:fileID
                                                    downloadState:0
                                                       fileHandle:0
                                                          fileUri:nil];
    if([updateResponse isSuccess]){
      NSLog(@"File entry updated successfully");
    } else {
      NSLog(@"Error while updating file entry");
    }
    
    resolver([[Response errorResponseWithMessage:@"File has been removed from file system."] toDictionary]);
    return;
  }
  resolver([[Response successResponse] toDictionary]);
}

RCT_REMAP_METHOD(listSettings,
                 listSettingsWithSettingsId: (NSString *) settingsId
                 withResolver: (RCTPromiseResolveBlock) resolver
                 andRejecter: (RCTPromiseRejectBlock) rejecter){
  SettingsDbo *settingsDbo = [[self settingsRepository] getById: settingsId];
  
  NSDictionary *settingsDict = [[settingsDbo toModel] toDictionary];
  NSString *serializedSettings = [DictionaryUtils convertToJsonWithDictionary: settingsDict];
  
  SingleResponse *response = [SingleResponse successSingleResponseWithResult: serializedSettings];
  
  resolver([response toDictionary]);
}

RCT_REMAP_METHOD(insertSyncSetting,
                 insertSettingsWithSettingsId: (NSString *) settingsId
                 withResolver: (RCTPromiseResolveBlock) resolver
                 andRejecter: (RCTPromiseRejectBlock) rejecter)
{
  [NSUserDefaults.standardUserDefaults setObject:settingsId forKey:@"email"];
  Response *insertResult = [[self settingsRepository] insertById:settingsId];
  
  resolver([insertResult toDictionary]);
}

RCT_REMAP_METHOD(updateSyncSettings,
                 updateSettingsWithSettingsId: (NSString *) settingsId
                 syncSettings: (int) syncSettings
                 withResolver: (RCTPromiseResolveBlock) resolver
                 andRejecter: (RCTPromiseRejectBlock) rejecter)
{
  resolver([[[self settingsRepository] updateById: settingsId
                                     syncSettings: syncSettings] toDictionary]);
}

RCT_REMAP_METHOD(setFirstSignIn, setFirstSighInWithSettingsId: (NSString *) settingsId
                 syncSettings: (int) syncSettings
                 withResolver: (RCTPromiseResolveBlock) resolver
                 andRejecter: (RCTPromiseRejectBlock) rejecter)
{
  resolver([[[self settingsRepository] updateById: settingsId
                                     syncSettings: syncSettings
                                       firtSignIn: NO] toDictionary]);
}

RCT_REMAP_METHOD(changeSyncStatus,
                 changeSyncStatusWithSettingId: (NSString *) settingId
                 value: (BOOL) value
                 withResolver: (RCTPromiseResolveBlock) resolver
                 andRejecter: (RCTPromiseRejectBlock) rejecter)
{
  if(!settingId)
  {
    resolver([[Response errorResponseWithMessage:@"SettingId is not specified"] toDictionary]);
    
    return;
  }
  
  [[STSyncScheduler sharedInstance] cancelSchedule];

  SettingsDbo *settingDbo = [[self settingsRepository] getById: settingId];
  
  if(!settingDbo)
  {
    resolver([[Response errorResponseWithMessage: @"No setting entry for current account"]
              toDictionary]);
    
    return;
  }
  

  STSettingsModel *settingsModel = [settingDbo toModel];
  int settings = [settingsModel syncSettings];
  
  if(value)
  {
    settings = settings | (SyncON | SyncPhotos);
    [[STSyncScheduler sharedInstance] startSyncDelayed];
  }
  else
  {
    settings = ~SyncON & settings;
  }
  
  [settingDbo setSyncStatus: value];
  [settingDbo setSyncSettings: settings];
  
  Response *result = [[self settingsRepository] update: [settingDbo toModel]];
  resolver([result toDictionary]);
}

//WithResolver: (RCTPromiseResolveBlock) resolver
//andRejecter: (RCTPromiseRejectBlock) rejecter){
//  [MethodHandler
//   invokeParallelWithParams:@{@RESOLVER: resolver,
//                               @REJECTER : rejecter}
//   andMethodHandlerBlock:^(NSDictionary * _Nonnull param) {
//
//   }];
//}

@end
