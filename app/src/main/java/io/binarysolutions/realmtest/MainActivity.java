package io.binarysolutions.realmtest;

import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import io.binarysolutions.realmtest.databinding.ActivityMainBinding;
import io.binarysolutions.realmtest.model.Contact;
import io.binarysolutions.realmtest.model.Message;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    /**
     * Example configurations
     * Realm versions: 3.0.0
     * N = Maximum global instances
     *
     * One write and one read thread with copyFromRealm will crash on <1000 write iterations N=3
     *
     * NUM_CONTACTS=24
     * NUM_MESSAGES=1000
     * UI_REALM=true
     * NUM_WRITE_THREADS=1
     * NUM_CALLBACKS=0
     * NUM_READ_THREADS=1
     * COPY_FROM_REALM=true
     *
     */


    //region config
    /**
     * Number of contacts to pre-populate in the database
     */
    public static final int NUM_CONTACTS = 24;

    /**
     * Number of messages to add on each contact
     */
    public static final int NUM_MESSAGES = 1000;

    /**
     * Leave a Realm open on the UI thread
     */
    private static final boolean UI_REALM = true;

    /**
     * Number of threads to have write transactions concurrently
     */
    private static final int NUM_WRITE_THREADS = 2;

    /**
     * Number of Rx callbacks to run write transactions on (Schedulers.computation())
     */
    private static final int NUM_CALLBACKS = 0;

    /**
     * Number of threads to have reads concurrently
     */
    private static final int NUM_READ_THREADS = 1;

    /**
     * Do we copyFromRealm in the read threads?
     */
    private static final boolean COPY_FROM_REALM = true;
    //endregion




    public static final RealmConfiguration REALM_CONFIG = new RealmConfiguration.Builder()
            .name("test.realm")
            .encryptionKey(new byte[] {-28, -13, -114, 48, 62, 27, 14, 126, -114, -38, -125, 83, 41, -52, -34, 116, -114, -99, 96, -71, 51, -44, 118, -45, 41, 78, 1, -107, -23, -70, -101, 86, -96, -46, -39, -29, 42, 103, -76, 87, -119, -10, -7, -52, 45, -87, -47, 126, 86, 26, 73, -26, 108, 49, -10, 19, -81, -9, -106, -99, 126, -78, -35, -71})
            .deleteRealmIfMigrationNeeded()
            .build();

    private Realm mRealm;

    private ActivityMainBinding mBinding;

    private volatile boolean mRunThreads = true;

    private List<Thread> mThreads = Collections.emptyList();

    private SerializedSubject<String, String> onContactUpdate = new SerializedSubject<>(PublishSubject.<String>create());

    private AtomicInteger globalCommits = new AtomicInteger(0);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        if(getActionBar() != null) {
            getActionBar().hide();
        }

        if(mRealm == null) {
            Realm.deleteRealm(REALM_CONFIG);
            mRealm = Realm.getInstance(REALM_CONFIG);
        }

        // Clear the db before beginning.
        try (Realm realm = Realm.getInstance(REALM_CONFIG)) {
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    realm.deleteAll();
                }
            });
        }

        mRealm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                for (int i = 0; i < NUM_CONTACTS; i++) {
                    Contact.createContact(realm, NUM_MESSAGES, i);
                }
            }
        }, new Realm.Transaction.OnSuccess() {
            @Override
            public void onSuccess() {
                runOnUiThread(new TimerTask() {
                    @Override
                    public void run() {
                        if(mBinding != null) {
                            Button startButton = mBinding.startButton;
                            startButton.setEnabled(true);
                            startButton.setText(R.string.start_test_title);
                            startButton.setBackgroundResource(R.color.colorPrimary);
                        }
                        if (!UI_REALM) mRealm.close();
                    }
                });
            }
        });


        for(int x = 0; x < NUM_CALLBACKS; ++x) {
            onContactUpdate
                    .onBackpressureBuffer()
                    .observeOn(Schedulers.computation())
                    .subscribe(new Action1<String>() {
                        @Override
                        public void call(String s) {
                            try (Realm realm = Realm.getInstance(REALM_CONFIG)) {
                                //Do bad things like access the object outside of the transaction
                                final Contact c = realm.where(Contact.class).equalTo("id", s).findFirst();

                                realm.executeTransaction(new Realm.Transaction() {
                                    @Override
                                    public void execute(Realm realm) {
                                        c.setNumber(new Random().nextInt() + "");

                                        globalCommits.incrementAndGet();
                                    }
                                });

                                globalCommits.incrementAndGet();
                            }
                        }
                    });
        }


        mBinding.startButton.setOnClickListener(this);
        mBinding.setThreadOneCount("0");
        mBinding.setThreadTwoCount("0");
        mBinding.setStartTime("");
        mBinding.setDuration("");
        mBinding.setErrorMessages("");
        mBinding.notifyChange();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (!mRealm.isClosed()) {
            mRealm.close();
        }
        mRealm = null;
    }

    @Override
    public void onClick(View view) {
        //we are going to create a bunch of threads, and pound on the realm db

        if (mThreads == null || mThreads.isEmpty()) {
            //we are going to create new threads

            final long startTime = System.currentTimeMillis();
            final AtomicInteger i = new AtomicInteger(0);
            final AtomicInteger writes = new AtomicInteger(0);
            final AtomicInteger reads = new AtomicInteger(0);
            mThreads = new ArrayList<>(NUM_WRITE_THREADS + NUM_READ_THREADS);

            for (int x=0; x < NUM_WRITE_THREADS; ++x) {
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while(mRunThreads) {
                            try (Realm realm = Realm.getInstance(REALM_CONFIG)) {
                                long runtime = System.currentTimeMillis() - startTime;

                                mBinding.setThreadOneCount("" + writes.incrementAndGet());
                                mBinding.setDuration(Long.toString(runtime));
                                mBinding.notifyChange();
                                int global = Realm.getGlobalInstanceCount(REALM_CONFIG);
                                int local = Realm.getLocalInstanceCount(REALM_CONFIG);

                                Log.i("Writer", "Global: " + global + ", Local: " + local + ", Count: " + writes.get() + ", Duration: " + runtime);

                                realm.executeTransaction(new Realm.Transaction() {
                                    @Override
                                    public void execute(Realm realm) {
                                        Contact contact = realm.where(Contact.class).equalTo("id", Contact.CONTACT_ID_START + 0).findFirst();

                                        contact.setUsername("User" + i.incrementAndGet());
                                        onContactUpdate.onNext(contact.getId());

                                        globalCommits.incrementAndGet();
                                    }
                                });

                                if ((i.get() % 1000) == 0) {
                                    Log.i("Writer", i.get() + " thread updates [" + Thread.currentThread().getName() + "]");
                                    Log.i("Writer", globalCommits.get() + " Total commits");
                                }
                            }
                        }
                    }
                });
                t.setDaemon(true);
                mThreads.add(t);
            }

            //make some reader threads
            for (int x=0; x < NUM_READ_THREADS; ++x) {
                Thread t = new Thread(new TimerTask() {
                    @Override
                    public void run() {
                        while(mRunThreads) {
                            try (Realm realm = Realm.getInstance(REALM_CONFIG)) {
                                //lets do some other reads to keep realm busy
                                long runtime = System.currentTimeMillis() - startTime;

                                mBinding.setThreadTwoCount("" + reads.getAndIncrement());
                                mBinding.notifyChange();
                                int global = Realm.getGlobalInstanceCount(REALM_CONFIG);
                                int local = Realm.getLocalInstanceCount(REALM_CONFIG);
                                Log.i("Reader", "Global: " + global + ", Local: " + local + ", Count: " + reads.get() + ", Duration: " + runtime);

                                // Do some read operations
                                Contact c = realm.where(Contact.class).equalTo("id", Contact.CONTACT_ID_START + 0).findFirst();
                                if (!c.getMessage(0).getBody().contains(Message.LORUM_IPSUM)) {
                                    Log.e("Reader", "Message body doesn't match");
                                }
                                if (COPY_FROM_REALM) {
                                    Contact detatched = realm.copyFromRealm(c);
                                    if (!detatched.getMessage(0).getBody().contains(Message.LORUM_IPSUM)) {
                                        Log.e("Reader", "Message body doesn't match");
                                    }
                                }
                            }
                        }
                    }
                });
                t.setDaemon(true);
                mThreads.add(t);
            }

            mRunThreads = true;

            //Start all the threads
            for (Thread t: mThreads) {
                t.start();
            }

            mBinding.startButton.setText("Stop Test");
        } else {
            mRunThreads = false;

            for (Thread t: mThreads) {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            mThreads = Collections.emptyList();

            mBinding.startButton.setText("Start Test");
        }


    }
}
