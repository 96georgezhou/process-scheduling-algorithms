// -----------------------------------------------------------------------------
// -----------------------------Written by Josh Brunner-------------------------
// ------------------------for CSS 430 HW2/pt.2  Assignment---------------------
// ---------------------------Last modified: 4/28/2014--------------------------
// --------------------------------Scheduler.java-------------------------------
/*
 * PURPOSE OF FILE
 * This file serves to perform the scheduling algorithm as required by the 
 * CSS 430's Program 2: Scheduling assignment. It implements a multilevel feed 
 * back-queue scheduler by using three separate queues. Each queue allows the 
 * processes within to execute for certain time as specified by the global 
 * variable, timeSlice.
 *
 * A generic description of the multilevel feed back-queue scheduler algorithm:
 * 1. It has three queues (queue0, queue1, and queue2).
 * 2. A new thread's TCB is always enqueued into queue0.
 * 3. Your scheduler first executes all threads in queue0.
 * 4. If a thread in the queue0 does not complete its execution for queue0's
 *    time slice, the scheduler moves the corresponding TCB to queue1.
 * 5. If queue0 is empty, it will execute threads in queue1. However, in order
 *    to react new threads in queue0, this scheduler executes a thread in queue1
 *    for timeSlice / 2 and then checks if queue0 has new TCBs. If so, it will 
 *    execute all threads in queue0 first, and thereafter resumes the execution 
 *    of the same thread in queue1 for another timeSlice / 2.
 * 6. If a thread in queue 1 does not complete its execution for queue1's time
 *    quantum, the scheduler then moves the TCB to queue2.
 * 7. If both queue0 and queue1 is empty, it can execute threads in queue2. 
 *    However, in order to react threads with higher priority in queue0 and 
 *    queue1, your scheduler should execute a thread in queue2 for 
 *    timeSlice / 2 and then check if queue0 and queue1 have new TCBs.
 * 8. If a thread in queue 2 does not complete its execution for queue2's time
 *    slice, the scheduler puts it back to the tail of queue2.
 *
 * BRIEF NOTE
 * Above each of the following functions, there's a breif description of the 
 * function's job as well as what other functions it might call it.
 *
 * ASSUMPTIONS
 * It is assumed that the user knows that the default execution of these 
 * processes is 1000ms, or 1 second. Upon receiving ThreadOS's input prompt
 * "-->" the user will load, with "l", and type the program's name "Test2".
 * 
 * The full input example is: "--> l Test2"
 */

import java.util.*;

public class Scheduler extends Thread {
    private Vector queue0;                           //top level priority
    private Vector queue1;                           //mid-level priority
    private Vector queue2;                           //low-level priority
    private int timeSlice;
    private static final int DEFAULT_TIME_SLICE = 1000;
    private static final int DEFAULT_MAX_THREADS = 10000;
    private boolean[] tids;                          //Keep track of in-use Tids
    private int nextId = 0;

    // -------------------------------------------------------------------------
    // getMyTcb( )
    /*
     * SUMMARY
     * This function is responsible for retrieving the current thread's TCB from
     * the right queue. It starts by analyzing queue0 and proceeds onto queue1 
     * and queue2. Upon finding the TCB, it returns the object to the function 
     * that called it. Otherwise, it returns null.
     */
    public TCB getMyTcb( ) {
        Thread myThread = Thread.currentThread( );      //Get thread object
        
        synchronized( queue0 ) {                        //Look for TCB in queue0
            for ( int i = 0; i < queue0.size( ); i++ ) {
                TCB tcb = ( TCB )queue0.elementAt( i ); //Grab a TCB object
                Thread thread = tcb.getThread( );       //Grab the TCB's thread
                if ( thread == myThread ) return tcb;   //Check if its the one
            }
        }
        synchronized( queue1 ) {                        //Look for TCB in queue1
            for ( int i = 0; i < queue1.size( ); i++ ) {
                TCB tcb = ( TCB )queue1.elementAt( i );
                Thread thread = tcb.getThread( );
                if ( thread == myThread ) return tcb;
            }
        }
        synchronized( queue2 ) {                        //Look for TCB in queue2
            for ( int i = 0; i < queue2.size( ); i++ ) {
                TCB tcb = ( TCB )queue2.elementAt( i );
                Thread thread = tcb.getThread( );
                if ( thread == myThread ) return tcb;
            }
        }
        return null;
    }
    
    // -------------------------------------------------------------------------
    // initTid( int maxThreads )
    /*
     * SUMMARY
     * This function is responsible for allocating an ID array. As it goes 
     * through a for loop, each element is marked to indicate if that id has 
     * been used.
     */
    private void initTid( int maxThreads ) {
        tids = new boolean[maxThreads];
        for ( int i = 0; i < maxThreads; i++ ) {
            tids[i] = false;
        }
    }
    
    // -------------------------------------------------------------------------
    // getNewTid( )
    /*
     * SUMMARY
     * This function is responsible for searching an available thread ID and
     * providing a new thread with this ID.
     */
    private int getNewTid( ) {
        for ( int i = 0; i < tids.length; i++ ) {
            int tentative = ( nextId + i ) % tids.length;
            if ( tids[tentative] == false ) {
                tids[tentative] = true;
                nextId = ( tentative + 1 ) % tids.length;
                return tentative;
            }
        }
        return -1;
    }
    
    // -------------------------------------------------------------------------
    // returnTid( int tid )
    /*
     * SUMMARY
     * This function is responbile for returnign the thread ID and set the
     * corresponding tids element to be unused.
     */
    private boolean returnTid( int tid ) {
        if ( tid >= 0 && tid < tids.length && tids[tid] == true ) {
            tids[tid] = false;
            return true;
        }
        return false;
    }
    
    // -------------------------------------------------------------------------
    // getMaxThreads( )
    /*
     * SUMMARY
     * This function is responsible for returning the maximal number of threads 
     * to be spawned in the system
     */
    public int getMaxThreads( ) {
        return tids.length;
    }
    
    // -------------------------------------------------------------------------
    // Scheduler()
    /*
     * SUMMARY
     * This is the default constructor for the scheduler class. It establishes 
     * and initializes the default values for timeSlice and initTid. In 
     * addition, it creates three priority queues for the TCB's to live in 
     * during their execution.
     */
    public Scheduler( ) {
        timeSlice = DEFAULT_TIME_SLICE;
        queue0 = new Vector( );
        queue1 = new Vector( );
        queue2 = new Vector( );
        initTid( DEFAULT_MAX_THREADS );
    }

    // -------------------------------------------------------------------------
    // Scheduler(int quantum)
    /*
     * SUMMARY
     * Similar to the constructor above, this constructor recieves a custom time
     * quantum. Altering this number will effect the execution time of 
     * processess throughout scheduler's use.
     */
    public Scheduler(int quantum) {
        timeSlice = quantum;
        queue0 = new Vector( );
        queue1 = new Vector( );
        queue2 = new Vector( );
        initTid( DEFAULT_MAX_THREADS );
    }

    // -------------------------------------------------------------------------
    // Scheduler(int quantum, int maxThreads)
    /*
     * SUMMARY
     * Similar constructor as the ones above. However, this constructor recieves
     * the maximum number of threads that can be spawned.
     */
    public Scheduler( int quantum, int maxThreads ) {
        timeSlice = quantum;
        queue0 = new Vector( );
        queue1 = new Vector( );
        queue2 = new Vector( );
        initTid( maxThreads );
    }

    // -------------------------------------------------------------------------
    // schedulerSleep( )
    /*
     * SUMMARY
     * This function is responsible for sleepign the scheduler to free up CPU 
     * time for the processes to execute. There is no ability to specify how 
     * much time the scheduler should sleep. Rather, it simply sleeps for the 
     * amount of time timeSlice has been told upon the scheduler's creation.
     */
    private void schedulerSleep( ) {
        try {
            Thread.sleep( timeSlice );
        } catch ( InterruptedException e ) {
        }
    }

    // -------------------------------------------------------------------------
    // addThread( Thread t )
    /*
     * SUMMARY
     * This function is responsible for adding threads to queue0. It begins with
     * getting the parent thread's TCB and Tid, creating a TCB object, and 
     * lastly adds this tcb to queue0. It returns this TCB object.
     */
    public TCB addThread( Thread t ) {
        TCB parentTcb = getMyTcb( );                    //Get TCB and find TID
        int pid = ( parentTcb != null ) ? parentTcb.getTid( ) : -1;
        int tid = getNewTid( );                         //Get a new TID
        
        if ( tid == -1) return null;
        
        TCB tcb = new TCB( t, tid, pid );               //Create a new TCB
        queue0.add( tcb );                              //Add it to queue0
        return tcb;
    }
    
    // -------------------------------------------------------------------------
    // deleteThread( )
    /*
     * SUMMARY
     * This function is responsible for removing the TCB of a terminating 
     * thread. Upon being called, it grabs the current TCB from the thread, 
     * it then checks to make sure it isn't null. If it isn't, it sets the 
     * tracker to "terminated" and returns. Otherwise, it returns false.
     */
    public boolean deleteThread( ) {
        TCB tcb = getMyTcb( );
        if ( tcb!= null ) {
            return tcb.setTerminated( );
        } else {
            return false;
        }
    }
    
    // -------------------------------------------------------------------------
    // sleepThread( int milliseconds )
    /*
     * SUMMARY
     * This function is responsible for sleeping a current thread by a 
     * user-specified time amount. Throughout tihs program, each queue is 
     * allowed to sleep for a specific time - timeSlice.
     */
    public void sleepThread( int milliseconds ) {
        try {
            sleep( milliseconds );
        } catch ( InterruptedException e ) { }
    }
    
    // -------------------------------------------------------------------------
    // run()
    /*
     * SUMMARY
     * Since Scheduler extends Thread, this run() function is called 
     * automatically when ThreadOS starts. It starts out by creating a Thread
     * variable, current. It imediately enters into a while loop that runs 
     * indefinetly. It is responsible for managing three differetn queues 
     * (queue0, queue1, and queue2). It does so by calling three separate 
     * process functions to handle each queue depending on if certain metrics 
     * are met.
     */
    public void run( ) {
        Thread current = null;
        while ( true ) {
            try {
                if(allQueuesAreEmpty()) continue;        //Back to top
                
                if(queue0_hasContent()){
                    if(processQueue0(current)) continue; //Process queue0
                    continue;                            //Back to top
                }
                if(queue0_isEmpty() && queue1_hasContent()){
                    if(processQueue1(current)) continue; //Process queue1
                    continue;                            //Back to top
                }
                if(queue0_isEmpty() && queue1_isEmpty() && queue2_hasContent()){
                    if(processQueue2(current)) continue; //Process queue2
                    continue;                            //Back to top
                }
            } catch ( NullPointerException e3 ) { };
        }
    }
    
    // -------------------------------------------------------------------------
    // processQueue0(Thread), processQueue1(Thread), processQueue2(Thread)
    /*
     * SUMMARY
     * The next three functions are similar yet different at the same time. For
     * the purpose of documentation, relevant comments are labeled on the side 
     * of the code. Essentially, they accept a Thread and get its TCB object. 
     * With this TCB object, it uses some supporting functions to do certain 
     * tasks that each function shares. The main difference that one might find 
     * between these functions is that they handle the processing of each queue
     * in specific ways.
     *
     * processQueue0
     * Allows the processes in queue0 to execute for timeSlice/2. Afterwords,
     * it moves incomplete processes into queue1 and returns.
     *
     * processQueue1
     * Allows processes in queue1 to execute for timeSlice. Afterwords, it moves
     * incomplete processes into queue1 and returns.
     *
     * processQueue2
     * The last queue in the list, queue2, is the ending place for those 
     * processes that do not finish within the alotted time from queue0 and 
     * queue1 - timeSlice*2. Afterwords, it moves the processes still in the 
     * queue from the front of the queue2 to the end of queue2.
     * 
     * SIDE NOTE
     * If a TCB finishes its CPU burst within this queue, it returns the 
     * function as true so that the rest of the processing code does not 
     * get executed.
     *
     */
    private boolean processQueue0(Thread current){
        TCB currentTCB = (TCB)queue0.firstElement( );   //grab queue's first TCB
        if(threadIsDead(currentTCB, queue0)) return true;
        
        current = currentTCB.getThread( );              //grab thread object
        getThreadGoing(current);                        //start/resume thread
        sleepThread(timeSlice/2);                       //sleep the scheduler
        
        //Move TCBs from queue0 to queue1
        finishProcessingQueue(queue0, queue1, current, currentTCB);
        return false;
    }
    
    private boolean processQueue1(Thread current){
        TCB currentTCB = (TCB)queue1.firstElement( );   //grab queue's first TCB
        if(threadIsDead(currentTCB, queue1)) return true;
        
        current = currentTCB.getThread( );              //grab thread object
        getThreadGoing(current);                        //start/resume thread
        
        sleepThread(timeSlice/2);                       //first timeSlice/2
        if(queue0_hasContent()) processNewTcb(current);
        sleepThread(timeSlice/2);                       //second timeSlice/2
        
        //Move TCBs from queue1 to queue2
        finishProcessingQueue(queue1, queue2, current, currentTCB);
        return false;
    }
    
    private boolean processQueue2(Thread current){
        TCB currentTCB = (TCB)queue2.firstElement( );   //grab queue's first TCB
        if(threadIsDead(currentTCB, queue2)) return true;
        
        current = currentTCB.getThread( );              //grab thread object
        getThreadGoing(current);                        //start/resume thread
        
        sleepThread(timeSlice/2);                       //first timeSlice/2
        if(queue0_hasContent() || queue1_hasContent()) processNewTcb(current);
        sleepThread(timeSlice/2);                       //second timeSlice/2
        sleepThread(timeSlice);                         //last timeSlice
        
        //Keep TCBs in queue2 so that their bursts can be completed.
        finishProcessingQueue(queue2, queue2, current, currentTCB);
        return false;
    }
    
    // -------------------------------------------------------------------------
    // processNewTcb(Thread current)
    /*
     * SUMMARY
     * For clarity's sake, this function provides the processQueue# with the 
     * ability to process any new TCB's added to the program.
     */
    private void processNewTcb(Thread current){
        if (current != null && current.isAlive()){
            current.suspend();                          //put thread to sleep
            Thread newProcess = null;                   //create new thread
            processQueue0(newProcess);                  //process new TCB
            current.resume();                           //resume the old TCB
        }
    }
    
    // -------------------------------------------------------------------------
    // finishProcessingQueue(Vector myQueue, Vector nextQueue,
    //                       Thread current, TCB currentTCB){
    /*
     * SUMMARY
     * This function is called by each of the processQueueX() functions. It is
     * responsible for finishing up the processing of each tcb. It accepts the 
     * starting and ending destination queue for the tcb object.
     */
    private void finishProcessingQueue(Vector myQueue, Vector nextQueue,
                                       Thread current, TCB currentTCB){
        synchronized ( myQueue ) {
            if ( current != null && current.isAlive( ) ) {
                current.suspend();                      //put thread to sleep
                myQueue.remove( currentTCB );           //remove TCB from queue0
                nextQueue.add(currentTCB);              //add TCB to queue1
            }
        }
    }
    
    // -------------------------------------------------------------------------
    // threadIsDead(TCB currentTCB, Vector queue)
    /*
     * SUMMARY
     * This is a neat function that lets the callign function know if the 
     * current thread has finished it's CPU burst. If it finds a TCB terminated
     * with its CPU burst, it removes it from the queue it's in and resets the 
     * Tid array s othat that Tid is freed up for further use.
     */
    private boolean threadIsDead(TCB currentTCB, Vector queue){
        if ( currentTCB.getTerminated( ) == true ) {    //if TCB is dead, run
            queue.remove( currentTCB );                 //remove TCB from queue
            returnTid( currentTCB.getTid( ) );          //update the Tid array
            return true;                               //return method call
        }
        return false;
    }
    
    // -------------------------------------------------------------------------
    // getThreadGoing(Thread current)
    /*
     * SUMMARY
     * No matter what state the thread is in, this function will either spin 
     * the thread up for the first time or simply resume its execution.
     */
    private void getThreadGoing(Thread current){
        if ( current != null ) {                        //No null threads!
            if ( current.isAlive( ) ) {                 //if thread is suspened,
                current.resume();                       //resume the thread
            } else {                                    //Otherwise,
                current.start( );                       //'spin' it up
            }
        }
    }
    
    // -------------------------------------------------------------------------
    // boolean helper functions
    /*
     * SUMMARY
     * The following list of functions provide better readability in the code 
     * above. Their purpose is easily determined and are primarily implemented 
     * because I care about code readability.
     */
    private boolean queue0_hasContent(){ return (queue0.size() > 0); }
    private boolean queue1_hasContent(){ return (queue1.size() > 0); }
    private boolean queue2_hasContent(){ return (queue2.size() > 0); }
    
    private boolean queue0_isEmpty(){ return (queue0.size() == 0); }
    private boolean queue1_isEmpty(){ return (queue1.size() == 0); }
    private boolean queue2_isEmpty(){ return (queue2.size() == 0); }
    
    private boolean allQueuesAreEmpty(){
        return (queue0_isEmpty() && queue1_isEmpty() && queue2_isEmpty());
    }
}
