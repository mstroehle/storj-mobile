package io.storj.mobile.storjlibmodule;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.Driver;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.storj.mobile.storjlibmodule.dataprovider.contracts.SynchronizationQueueContract;
import io.storj.mobile.storjlibmodule.dataprovider.dbo.SyncQueueEntryDbo;
import io.storj.mobile.storjlibmodule.dataprovider.repositories.SyncQueueRepository;
import io.storj.mobile.storjlibmodule.enums.DownloadStateEnum;
import io.storj.mobile.storjlibmodule.enums.SyncSettingsEnum;
import io.storj.mobile.storjlibmodule.enums.SyncStateEnum;
import io.storj.mobile.storjlibmodule.models.BucketModel;
import io.storj.mobile.storjlibmodule.models.FileModel;
import io.storj.mobile.storjlibmodule.models.SettingsModel;
import io.storj.mobile.storjlibmodule.models.SyncQueueEntryModel;
import io.storj.mobile.storjlibmodule.models.UploadingFileModel;
import io.storj.mobile.storjlibmodule.responses.Response;
import io.storj.mobile.storjlibmodule.responses.SingleResponse;
import io.storj.mobile.storjlibmodule.dataprovider.DatabaseFactory;
import io.storj.mobile.storjlibmodule.dataprovider.dbo.BucketDbo;
import io.storj.mobile.storjlibmodule.dataprovider.dbo.FileDbo;
import io.storj.mobile.storjlibmodule.dataprovider.dbo.SettingsDbo;
import io.storj.mobile.storjlibmodule.dataprovider.dbo.UploadingFileDbo;
import io.storj.mobile.storjlibmodule.dataprovider.contracts.SettingsContract;
import io.storj.mobile.storjlibmodule.dataprovider.repositories.BucketRepository;
import io.storj.mobile.storjlibmodule.dataprovider.repositories.FileRepository;
import io.storj.mobile.storjlibmodule.dataprovider.repositories.SettingsRepository;
import io.storj.mobile.storjlibmodule.dataprovider.repositories.UploadingFilesRepository;
import io.storj.mobile.storjlibmodule.services.SynchronizationSchedulerJobService;
import io.storj.mobile.storjlibmodule.services.SynchronizationService;

/**
 * Created by Yaroslav-Note on 3/9/2018.
 */

public class SyncModule extends ReactContextBaseJavaModule {
    private static final int KEEP_ALIVE_TIME = 1;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
    private static int NUMBER_OF_CORES =
            Runtime.getRuntime().availableProcessors();

    private final BlockingDeque<Runnable> mWorkQueue;
    private final ThreadPoolExecutor mThreadPool;
    
    public SyncModule(ReactApplicationContext reactContext) {
        
        super(reactContext);
        mWorkQueue = new LinkedBlockingDeque<>();
        mThreadPool = new ThreadPoolExecutor(
                NUMBER_OF_CORES,
                NUMBER_OF_CORES,
                KEEP_ALIVE_TIME,
                KEEP_ALIVE_TIME_UNIT,
                mWorkQueue
        );
    }

    @Override
    public String getName() {
        return "SyncModule";
    }

    @ReactMethod
    public void listBuckets(final String sortingMode, final Promise promise) {
        Log.d("SYNC DEBUG", "List buckets sort START");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try(SQLiteDatabase db = new DatabaseFactory(getReactApplicationContext(), null).getReadableDatabase()) {
                    BucketRepository bucketRepository = new BucketRepository(db);

                    ArrayList<BucketDbo> bucketDbos = sortingMode.equalsIgnoreCase("name")
                            ? (ArrayList)bucketRepository.getAllCollateNocase(sortingMode, true)
                            : (ArrayList)bucketRepository.getAll();

                    int length = bucketDbos.size();
                    BucketModel[] bucketModels = new BucketModel[length];

                    for(int i = 0; i < length; i++) {
                        bucketModels[i] = bucketDbos.get(i).toModel();
                    }

                    promise.resolve(new SingleResponse(true, toJson(bucketModels), null).toWritableMap());
                } catch (Exception e) {
                    promise.resolve(new SingleResponse(false, null, e.getMessage()).toWritableMap());
                }
                Log.d("SYNC DEBUG", "List buckets sort END");
            }
        }).run();
    }

    @ReactMethod
    public void listFiles(final String bucketId, final String sortingMode, final Promise promise) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(bucketId == null || bucketId.isEmpty()) {
                    promise.resolve(new Response(false,  "Bucket id is corrupted").toWritableMap());
                    return;
                }

                try(SQLiteDatabase db = new DatabaseFactory(getReactApplicationContext(), null).getReadableDatabase()) {

                    FileRepository fileRepository = new FileRepository(db);

                    List<FileDbo> fileDbos = sortingMode.equalsIgnoreCase("name")
                            ? fileRepository.getAllCollateNocase(bucketId, sortingMode, true)
                            : fileRepository.getAll(bucketId);

                    int length = fileDbos.size();
                    FileModel[] fileModels = new FileModel[length];

                    for(int i = 0; i < length; i++) {
                        fileModels[i] = fileDbos.get(i).toModel();
                    }

                    promise.resolve(new SingleResponse(true, toJson(fileModels), null).toWritableMap());
                } catch (Exception e) {
                    promise.resolve(new SingleResponse(false, null, e.getMessage()).toWritableMap());
                }
            }
        }).run();
    }

    @ReactMethod
    public void listAllFiles(final String sortingMode, final Promise promise) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try(SQLiteDatabase db = new DatabaseFactory(getReactApplicationContext(), null).getReadableDatabase()) {

                    FileRepository fileRepository = new FileRepository(db);

                    List<FileDbo> fileDbos = sortingMode.equalsIgnoreCase("name")
                            ? fileRepository.getAll(sortingMode, true)
                            : fileRepository.getAll();

                    int length = fileDbos.size();
                    FileModel[] fileModels = new FileModel[length];

                    for(int i = 0; i < length; i++) {
                        fileModels[i] = fileDbos.get(i).toModel();
                    }

                    promise.resolve(new SingleResponse(true, toJson(fileModels), null).toWritableMap());
                } catch (Exception e) {
                    promise.resolve(new SingleResponse(false, null, e.getMessage()).toWritableMap());
                }
            }
        }).run();
    }

    @ReactMethod
    public void listUploadingFiles(String bucketId, final Promise promise) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try(SQLiteDatabase db = new DatabaseFactory(getReactApplicationContext(), null).getReadableDatabase()) {
                    //SQLiteDatabase db = new DatabaseFactory(getReactApplicationContext(), null).getReadableDatabase();

                    UploadingFilesRepository fileRepository = new UploadingFilesRepository(db);

                    ArrayList<UploadingFileDbo> fileDbos = (ArrayList)fileRepository.getAll();

                    int length = fileDbos.size();
                    UploadingFileModel[] fileModels = new UploadingFileModel[length];

                    for(int i = 0; i < length; i++) {
                        fileModels[i] = new UploadingFileModel(fileDbos.get(i));
                    }

                    promise.resolve(new SingleResponse(true, toJson(fileModels), null).toWritableMap());
                } catch (Exception e) {
                    promise.resolve(new SingleResponse(false, null, e.getMessage()).toWritableMap());
                }
            }
        }).run();
    }

    @ReactMethod
    public void getUploadingFile(final String fileHandle, final Promise promise) {
        if (fileHandle == null) {
            promise.resolve(new SingleResponse(false, null, "Invalid fileHandle!").toWritableMap());
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try(SQLiteDatabase db = new DatabaseFactory(getReactApplicationContext(), null).getReadableDatabase()) {
                    UploadingFilesRepository repo = new UploadingFilesRepository(db);
                    UploadingFileModel model = repo.get(fileHandle);

                    if(model == null) {
                        promise.resolve(new SingleResponse(false, null, "Uploading file not found!").toWritableMap());
                    } else {
                        promise.resolve(new SingleResponse(true, toJson(model), null).toWritableMap());
                    }

                } catch (Exception e) {
                    promise.resolve(new SingleResponse(false, null, e.getMessage()).toWritableMap());
                }
            }
        }).run();
    }

    @ReactMethod
    public void getFile(final String fileId, final Promise promise) {
        if (fileId == null) {
            promise.resolve(new SingleResponse(false, null, "Invalid fileId!").toWritableMap());
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try(SQLiteDatabase db = new DatabaseFactory(getReactApplicationContext(), null).getReadableDatabase()) {
                    FileRepository repo = new FileRepository(db);
                    FileDbo model = repo.get(fileId);

                    if(model == null) {
                        promise.resolve(new SingleResponse(false, null, "File not found!").toWritableMap());
                    } else {
                        promise.resolve(new SingleResponse(true, toJson(model.toModel()), null).toWritableMap());
                    }

                } catch (Exception e) {
                    promise.resolve(new SingleResponse(false, null, e.getMessage()).toWritableMap());
                }
            }
        }).run();
    }



    @ReactMethod
    public void updateBucketStarred(final String bucketId, final boolean isStarred, final Promise promise) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try(SQLiteDatabase db = new DatabaseFactory(getReactApplicationContext(), null).getReadableDatabase()) {
                    BucketRepository bucketRepository = new BucketRepository(db);
                    promise.resolve(bucketRepository.update(bucketId, isStarred).toWritableMap());

                } catch (Exception e) {
                    promise.resolve(new Response(false, e.getMessage()).toWritableMap());
                }
            }
        }).run();
    }

    @ReactMethod
    public void updateFileStarred(final String fileId, final boolean isStarred, final Promise promise) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try(SQLiteDatabase db = new DatabaseFactory(getReactApplicationContext(), null).getReadableDatabase()) {
                    FileRepository fileRepository = new FileRepository(db);
                    promise.resolve(fileRepository.update(fileId, isStarred).toWritableMap());

                } catch (Exception e) {
                    promise.resolve(new Response(false, e.getMessage()).toWritableMap());
                }
            }
        }).run();
    }

    @ReactMethod
    public void listSettings(final String id, final Promise promise) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try(SQLiteDatabase db = new DatabaseFactory(getReactApplicationContext(), null).getReadableDatabase()) {
                    SettingsRepository settingsRepo = new SettingsRepository(db);
                    SettingsDbo settingsDbo = settingsRepo.get(id);

                    promise.resolve(new SingleResponse(settingsDbo != null, toJson(settingsDbo.toModel()), null).toWritableMap());

                } catch (Exception e) {
                    promise.resolve(new Response(false, e.getMessage()).toWritableMap());
                }
            }
        }).run();
    }

    @ReactMethod
    public void insertSyncSetting(final String id, final Promise promise) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try(SQLiteDatabase db = new DatabaseFactory(getReactApplicationContext(), null).getReadableDatabase()) {
                    SettingsRepository settingsRepo = new SettingsRepository(db);
                    promise.resolve(settingsRepo.insert(id).toWritableMap());

                } catch (Exception e) {
                    promise.resolve(new Response(false, e.getMessage()).toWritableMap());
                }
            }
        }).run();
    }

    @ReactMethod
    public void updateSyncSettings(final String id, final int syncSettings, final Promise promise) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try(SQLiteDatabase db = new DatabaseFactory(getReactApplicationContext(), null).getReadableDatabase()) {
                    SettingsRepository settingsRepo = new SettingsRepository(db);
                    promise.resolve(settingsRepo.update(id, syncSettings).toWritableMap());

                } catch (Exception e) {
                    promise.resolve(new Response(false, e.getMessage()).toWritableMap());
                }
            }
        }).run();
    }

    @ReactMethod
    public void setFirstSignIn(final String id, final int syncSettings, final Promise promise) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try(SQLiteDatabase db = new DatabaseFactory(getReactApplicationContext(), null).getReadableDatabase()) {
                    SettingsRepository settingsRepo = new SettingsRepository(db);
                    promise.resolve(settingsRepo.update(id, syncSettings, false).toWritableMap());

                } catch (Exception e) {
                    promise.resolve(new Response(false, e.getMessage()).toWritableMap());
                }
            }
        }).run();
    }

    @ReactMethod
    public void changeSyncStatus(final String id, final boolean value, final Promise promise) {
        if(id == null) {
            promise.resolve(new Response(false, "settingId is not specified!"));
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try(SQLiteDatabase db = new DatabaseFactory(getReactApplicationContext(), null).getWritableDatabase()){
                    Driver driver = new GooglePlayDriver(getReactApplicationContext());
                    FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(driver);
                    dispatcher.cancelAll();
                    cancelSync();

                    SettingsRepository settingsRepo = new SettingsRepository(db);
                    SettingsDbo settingsDbo = settingsRepo.get(id);

                    if(settingsDbo == null) {
                        promise.resolve(new Response(false, "No settings entry for current account!"));
                        return;
                    }

                    SettingsModel settingsModel = settingsDbo.toModel();

                    if(value) {
                        scheduleSync(settingsModel, dispatcher);
                        Log.d("SYNC MODULE", "changeSyncStatus: Scheduled succesfully!");
                    }

                    promise.resolve(settingsRepo.update(id, value).toWritableMap());
                    Log.d("SYNC MODULE", "changeSyncStatus: settings entry updated successfully!");

                } catch(Exception e) {
                    promise.resolve(new Response(false, "Something went wrong! " + e.getMessage()).toWritableMap());
                    Log.d("SYNC MODULE", "changeSyncStatus: Error " + e.getMessage());
                }
            }
        }).run();
    }

    private void cancelSync() {
        Intent cancelSyncIntent = new Intent(getReactApplicationContext(), SynchronizationService.class);
        cancelSyncIntent.setAction(SynchronizationService.ACTION_SYNC_CANCEL);

        getReactApplicationContext().startService(cancelSyncIntent);
    }

    @ReactMethod
    public void getSyncQueue(final Promise promise) {
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try(SQLiteDatabase db = new DatabaseFactory(getReactApplicationContext(), null).getWritableDatabase()) {
                    SyncQueueRepository syncRepo = new SyncQueueRepository(db);
                    List<SyncQueueEntryModel> models = syncRepo.getAll();

                    promise.resolve(new SingleResponse(true, toJson(models), null).toWritableMap());
                } catch (Exception e) {
                    promise.resolve(new Response(false, e.getMessage()).toWritableMap());
                }
            }
        });
    }

    @ReactMethod
    public void getSyncQueueEntry(final int id, final Promise promise) {
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try(SQLiteDatabase db = new DatabaseFactory(getReactApplicationContext(), null).getWritableDatabase()) {
                    SyncQueueRepository syncRepo = new SyncQueueRepository(db);
                    SyncQueueEntryModel model = syncRepo.get(id);

                    if(model == null)
                        throw new Exception("Sync entrie not found");

                    promise.resolve(new SingleResponse(true, toJson(model), null).toWritableMap());
                } catch (Exception e) {
                    promise.resolve(new Response(false, e.getMessage()).toWritableMap());
                }
            }
        });
    }

    @ReactMethod
    public void updateSyncQueueEntryFileName(final int id, final String newFileName, final Promise promise) {
        if(newFileName == null)
            promise.resolve(new Response(false, "File name can't be null!"));

        updateSyncEntry(id, promise, new SetDboPropCallback() {
            @Override
            public void setProp(SyncQueueEntryDbo dbo) {
                dbo.setProp(SynchronizationQueueContract._STATUS, SyncStateEnum.IDLE.getValue());
                dbo.setProp(SynchronizationQueueContract._FILE_NAME, newFileName);
            }
        });
    }

    @ReactMethod
    public void updateSyncQueueEntryStatus(final int id, final int newStatus, final Promise promise) {
       updateSyncEntry(id, promise, new SetDboPropCallback() {
           @Override
           public void setProp(SyncQueueEntryDbo dbo) {
               List<SyncStateEnum> enums = Arrays.asList(SyncStateEnum.values());

               if(enums.contains(SyncStateEnum.valueOf(newStatus))) {
                   dbo.setProp(SynchronizationQueueContract._STATUS, newStatus);
               }
           }
       });
    }

    private interface SetDboPropCallback {
        void setProp(SyncQueueEntryDbo dbo);
    }

    private void updateSyncEntry(final int id, final Promise promise, final SetDboPropCallback callback) {
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try(SQLiteDatabase db = new DatabaseFactory(getReactApplicationContext(), null).getWritableDatabase()) {
                    SyncQueueRepository syncRepo = new SyncQueueRepository(db);
                    SyncQueueEntryModel model = syncRepo.get(id);

                    if(model == null)
                        throw new Exception("No entrie has been found!");

                    if(model.getStatus() == SyncStateEnum.PROCESSING.getValue() || model.getStatus() == SyncStateEnum.QUEUED.getValue())
                        throw new Exception("Can't update processing entry");

                    SyncQueueEntryDbo dbo = new SyncQueueEntryDbo(model);
                    callback.setProp(dbo);

                    model = dbo.toModel();
                    Response response = syncRepo.update(model);

                    if(!response.isSuccess())
                        throw new Exception(response.getError().getMessage());

                    promise.resolve(new SingleResponse(true, toJson(model), null).toWritableMap());
                } catch (Exception e) {
                    promise.resolve(new Response(false, e.getMessage()).toWritableMap());
                }
            }
        });
    }

    @ReactMethod
    public void checkFile(final String fileId, final String localPath, final Promise promise) {
        if(localPath == null) {
            promise.resolve(new Response(false, "localPath is null!").toWritableMap());
            Log.d("SYNC MODULE", "checkImage: Error local path is null!");
            return;
        }

        File file = new File(localPath);

        if(!file.exists() || file.isDirectory()) {
            Log.d("SYNC MODULE", "checkImage: File has been removed from file System!");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try(SQLiteDatabase db = new DatabaseFactory(getReactApplicationContext(), null).getWritableDatabase()) {
                        FileRepository fileRepo = new FileRepository(db);

                        Response updateResponse = fileRepo.update(fileId, DownloadStateEnum.DEFAULT.getValue(), 0, null);

                        if(!updateResponse.isSuccess()) {
                            Log.d("SYNC MODULE", "checkImage: Error while updating file entry");
                        } else {
                            Log.d("SYNC MODULE", "checkImage: File entry updated successfully");
                        }
                    } catch(Exception e) {
                        Log.d("SYNC MODULE", "checkImage: Error while updating file entry, exception: " + e.getMessage());
                    }
                }
            }).run();

            promise.resolve(new Response(false, "File has been removed from file System!").toWritableMap());
            return;
        }

        promise.resolve(new Response(true, null).toWritableMap());
    }

    private void scheduleSync(SettingsModel settingsModel, FirebaseJobDispatcher dispatcher) {
        //dispatcher.cancelAll();

        Bundle bundle = new Bundle();
        bundle.putString(SettingsContract._SETTINGS_ID, settingsModel.getId());

        List<Integer> constraints = new ArrayList<Integer>();
        int syncSettings = settingsModel.getSyncSettings();

        if(checkSyncSettings(syncSettings, SyncSettingsEnum.ON_WIFI.getValue())) {
            constraints.add(Constraint.ON_UNMETERED_NETWORK);
        }

        if(checkSyncSettings(syncSettings, SyncSettingsEnum.ON_CHARGING.getValue())) {
            constraints.add(Constraint.DEVICE_CHARGING);
        }

        Job myJob = getJobBuilder(dispatcher, bundle, constraints).build();
        dispatcher.schedule(myJob);
    }

    private boolean checkSyncSettings(int syncSettings, int syncValue) {
        return (syncSettings & syncValue) == syncValue;
    }

    private Job.Builder getJobBuilder(FirebaseJobDispatcher dispatcher, Bundle bundle, List<Integer> constaraints) {
        Job.Builder myJobBuilder = dispatcher.newJobBuilder()
                // the JobService that will be called
                .setService(SynchronizationSchedulerJobService.class)
                // uniquely identifies the job
                .setTag("sync-job")
                // one-off job
                .setRecurring(false)
                // don't persist past a device reboot
                .setLifetime(Lifetime.UNTIL_NEXT_BOOT)
                // start between 0 and 15 minutes (900 seconds)
                .setTrigger(Trigger.executionWindow(60, 120))
                // overwrite an existing job with the same tag
                .setReplaceCurrent(true)
                // retry with exponential backoff
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                .setExtras(bundle);

        int constraintSize = constaraints.size();

        if(constraintSize > 0) {
            //Integer[] intArray = (Integer[])constaraints.toArray();
            int[] constArray = new int[constraintSize];

            for(int i = 0; i < constraintSize; i++) {
                constArray[i] = constaraints.get(i);
            }

            myJobBuilder.setConstraints(constArray);
        }

        return myJobBuilder;
    }

    private <T> String toJson(T[] convertible) {
        return GsonSingle.getInstanse().toJson(convertible);
    }

    private <T> String toJson(T convertible) {
        return GsonSingle.getInstanse().toJson(convertible);
    }
}
