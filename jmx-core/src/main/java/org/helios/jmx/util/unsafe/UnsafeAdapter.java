/**
 * Helios Development Group LLC, 2013
 */
package org.helios.jmx.util.unsafe;


import java.io.File;
import java.io.RandomAccessFile;
import java.lang.management.ManagementFactory;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

import org.cliffc.high_scale_lib.NonBlockingHashMapLong;
import org.helios.jmx.util.helpers.JMXHelper;
import org.helios.jmx.util.reference.ReferenceRunnable;
import org.helios.jmx.util.reference.ReferenceService;
import org.helios.jmx.util.unsafe.Callbacks.BooleanCallable;
import org.helios.jmx.util.unsafe.Callbacks.ByteCallable;
import org.helios.jmx.util.unsafe.Callbacks.DoubleCallable;
import org.helios.jmx.util.unsafe.Callbacks.IntCallable;
import org.helios.jmx.util.unsafe.Callbacks.LongCallable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.misc.Unsafe;

/**
 * <p>Title: UnsafeAdapter</p>
 * <p>Description: Adapter for {@link sun.misc.Unsafe} that detects the version and provides adapter methods for
 * the different supported signatures.</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.util.unsafe.UnsafeAdapter</code></p>
 */
@SuppressWarnings({"javadoc", "restriction"})
public class UnsafeAdapter {
    /** The unsafe instance */    
	public static final Unsafe UNSAFE;
    /** The address size */
    public static final int ADDRESS_SIZE;
    /** Byte array offset */
    public static final int BYTES_OFFSET;
    /** Object array offset */
    public static final long OBJECTS_OFFSET;
    
    
    /** Indicates if the 5 param copy memory is supported */
    public static final boolean FIVE_COPY;
    /** Indicates if the 4 param set memory is supported */
    public static final boolean FOUR_SET;
    /** The size of a <b><code>byte</code></b>  */
    public final static int BYTE_SIZE = 1;
    /** The size of a <b><code>boolean</code></b>  */
    public final static int BOOLEAN_SIZE = 1;
    
    /** The size of a <b><code>char</code></b>  */
    public final static int CHAR_SIZE = 2;

    /** The size of an <b><code>int</code></b>  */
    public final static int INT_SIZE = 4;
    /** The size of an <b><code>int[]</code></b> array offset */
    public final static int INT_ARRAY_OFFSET;
    /** The size of a <b><code>long</code></b>  */
    public final static int LONG_SIZE = 8;    
    /** The size of a <b><code>long[]</code></b> array offset */
    public final static int LONG_ARRAY_OFFSET;
    /** The size of a <b><code>double</code></b>  */
    public final static int DOUBLE_SIZE = 8;    
    
    /** The size of a <b><code>double[]</code></b> array offset */
    public final static int DOUBLE_ARRAY_OFFSET;
    
    /** The size of a <b><code>byte[]</code></b> array offset */
    public final static int BYTE_ARRAY_OFFSET;
    /** The size of a <b><code>char[]</code></b> array offset */
    public final static int CHAR_ARRAY_OFFSET;
    
    /** A zero byte value const */
    public static final byte ZERO_BYTE = 0;
    /** A one byte value const */
    public static final byte ONE_BYTE = 1;
    
    /** The maximum size of a memory allocation request which can be aligned */
    public static final long MAX_ALIGNED_MEM = 1073741824;   // 1,073,741,824
    
    /** The JVM's OS Process ID (PID) */
    public static final long JVM_PID = Long.parseLong(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
	
	/** The configured native memory tracking enablement  */
	public static final boolean trackMem;
	/** The configured native memory alignment enablement  */
	public static final boolean alignMem;
	/** The unsafe memory management MBean */
	public static final UnsafeMemoryMBean unsafeMemoryStats;
	
	/** A map of memory allocation sizes keyed by the address */
	private static final NonBlockingHashMapLong<long[]> memoryAllocations;
	/** The total native memory allocation */
	private static final AtomicLong totalMemoryAllocated;
	/** The total native memory allocation overhead for alignment */
	private static final AtomicLong totalAlignmentOverhead;
	
	/** The system prop indicating that allocations should be tracked */
	public static final String TRACK_ALLOCS_PROP = "unsafe.allocations.track"; 
	/** The system prop indicating that allocations should be alligned */
	public static final String ALIGN_ALLOCS_PROP = "unsafe.allocations.align";
	
	/** The debug agent library */
	public static final String AGENT_LIB = "-agentlib:";
	
	/** The legacy debug agent library */
	public static final String LEGACY_AGENT_LIB = "-Xrunjdwp:";
	
    /** An empty map returned when state is requested by the management interface is disabled */
    private static final Map<String, Long> EMPTY_STAT_MAP = Collections.unmodifiableMap(new HashMap<String, Long>(0));
    /** The JVM's end of line character */
    public static final String EOL = System.getProperty("line.separator", "\n");
    
    
    /** The baseline allocation count */
    public static final int BASELINE_ALLOCS;
    /** The baseline allocation size */
    public static final long BASELINE_MEM;
    /** The baseline pending size */
    public static final int BASELINE_PENDING;
    /** The baseline alignment overhead */
    public static final long BASELINE_AO;
    
    
    //=========================================================================================================
    //			Memory Allocation Management
    //=========================================================================================================
    
    /** Empty long array const */
    private static final long[] EMPTY_LONG_ARR = {};
    /** Empty long[] array const */
    private static final long[][] EMPTY_ADDRESSES = {{}};
    /** Empty MemoryAllocationReference list const */
    private static final List<MemoryAllocationReference> EMPTY_ALLOC_LIST = Collections.unmodifiableList(new ArrayList<MemoryAllocationReference>(0));
    /** A map of memory allocation references keyed by an internal counter */
    protected static final NonBlockingHashMapLong<MemoryAllocationReference> deAllocs = new NonBlockingHashMapLong<MemoryAllocationReference>(1024, false);
    /** Serial number factory for memory allocationreferences */
    private static final AtomicLong refIndexFactory = new AtomicLong(0L);
    
	
	
	/**
	 * Determines if this JVM is running with the debug agent enabled
	 * @return true if this JVM is running with the debug agent enabled, false otherwise
	 */
	public static boolean isDebugAgentLoaded() {
		List<String> inputArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
		for(String s: inputArguments) {
			if(s.trim().startsWith(AGENT_LIB) || s.trim().startsWith(LEGACY_AGENT_LIB)) return true;
		}
		return false;
	}
	
	
	/**
	 * Simple out formatted logger
	 * @param fmt The format of the message
	 * @param args The message arguments
	 */
	public static void log(String fmt, Object...args) {
		System.out.println(String.format(fmt, args));
	}
    
    private UnsafeAdapter() {
    	
    }
    
    
    /**
     * <p>Title: UnsafeMemoryMBean</p>
     * <p>Description: JMX MBean interface for unsafe memory allocation trackers</p> 
     * <p>Company: Helios Development Group LLC</p>
     * @author Whitehead (nwhitehead AT heliosdev DOT org)
     * <p><code>org.helios.jmx.util.unsafe.UnsafeAdapter.UnsafeMemoryMBean</code></p>
     */
    public static interface UnsafeMemoryMBean {
    	
    	/** The map key for the total memory allocation in bytes */
    	public static final String ALLOC_MEM = "Memory";
    	/** The map key for the total memory allocation in KB */
    	public static final String ALLOC_MEMK = "MemoryKb";
    	/** The map key for the total memory allocation in MB */
    	public static final String ALLOC_MEMM = "MemoryMb";
    	/** The map key for the total number of current allocations */
    	public static final String ALLOC_COUNT = "Allocations";
    	/** The map key for the pending phantom references */
    	public static final String PENDING_COUNT = "Pending";
    	
    	/**
    	 * Returns the size in bytes of a native pointer
    	 * @return the size in bytes of a native pointer
    	 */
    	public int getAddressSize();
    	
    	/**
    	 * Returns the size in bytes of a native memory page
    	 * @return the size in bytes of a native memory page
    	 */
    	public int getPageSize();
    	
    	/**
    	 * Returns a map of unsafe memory stats keyed by the stat name
    	 * @return a map of unsafe memory stats
    	 */
    	public Map<String, Long> getState();
    	
    	/**
    	 * Returns the total off-heap allocated memory in bytes, not including the base line defined in {@link UnsafeAdapter#BASELINE_MEM}
    	 * @return the total off-heap allocated memory
    	 */
    	public long getTotalAllocatedMemory();
    	
    	/**
    	 * Returns the total aligned memory overhead in bytes
    	 * @return the total aligned memory overhead in bytes
    	 */
    	public long getAlignedMemoryOverhead();
    	
    	
    	/**
    	 * Returns the total off-heap allocated memory in Kb
    	 * @return the total off-heap allocated memory
    	 */
    	public long getTotalAllocatedMemoryKb();
    	
    	/**
    	 * Returns the total off-heap allocated memory in Mb
    	 * @return the total off-heap allocated memory
    	 */
    	public long getTotalAllocatedMemoryMb();

    	/**
    	 * Returns the total number of existing allocations, not including the base line defined in {@link UnsafeAdapter#BASELINE_ALLOCS}
    	 * @return the total number of existing allocations
    	 */
    	public int getTotalAllocationCount();
    	    	
    	/**
    	 * Returns the number of retained phantom references to memory allocations
    	 * @return the number of retained phantom references to memory allocations
    	 */
    	public int getPendingRefs();
    	
    }

    /**
     * <p>Title: InactiveUnsafeMemory</p>
     * <p>Description: Stub for unsafe memory allocation tracking when tracking is not enabled</p> 
     * <p>Company: Helios Development Group LLC</p>
     * @author Whitehead (nwhitehead AT heliosdev DOT org)
     * <p><code>org.helios.jmx.util.unsafe.UnsafeAdapter.InactiveUnsafeMemory</code></p>
     */
    public static class InactiveUnsafeMemory implements UnsafeMemoryMBean  {

    	/**
    	 * Returns a map of unsafe memory stats keyed by the stat name
    	 * @return a map of unsafe memory stats
    	 */
    	@Override
		public Map<String, Long> getState() {
			return Collections.EMPTY_MAP;
		}

    	/**
    	 * Returns the total off-heap allocated memory in bytes
    	 * @return the total off-heap allocated memory
    	 */
		@Override
		public long getTotalAllocatedMemory() {
			return -1L;
		}

    	/**
    	 * Returns the total aligned memory overhead in bytes
    	 * @return the total aligned memory overhead in bytes
    	 */
		@Override
		public long getAlignedMemoryOverhead() {
			return -1L;
		}

    	/**
    	 * Returns the total off-heap allocated memory in Kb
    	 * @return the total off-heap allocated memory
    	 */
		@Override
		public long getTotalAllocatedMemoryKb() {
			return -1L;
		}

    	/**
    	 * Returns the total off-heap allocated memory in Mb
    	 * @return the total off-heap allocated memory
    	 */
		@Override
		public long getTotalAllocatedMemoryMb() {
			return -1L;
		}

    	/**
    	 * Returns the total number of existing allocations
    	 * @return the total number of existing allocations
    	 */
		@Override
		public int getTotalAllocationCount() {
			return -1;
		}


    	/**
    	 * Returns the number of retained phantom references to memory allocations
    	 * @return the number of retained phantom references to memory allocations
    	 */
		@Override
		public int getPendingRefs() {			
			return -1;
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.jmx.util.unsafe.UnsafeAdapter.UnsafeMemoryMBean#getAddressSize()
		 */
		@Override
		public int getAddressSize() {
			return addressSize();
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.jmx.util.unsafe.UnsafeAdapter.UnsafeMemoryMBean#getPageSize()
		 */
		@Override
		public int getPageSize() {
			return pageSize();
		}
    	
    }
    
    /**
     * <p>Title: UnsafeMemory</p>
     * <p>Description: Management stats for unsafe memory allocation</p> 
     * <p>Company: Helios Development Group LLC</p>
     * @author Whitehead (nwhitehead AT heliosdev DOT org)
     * <p><code>org.helios.jmx.util.unsafe.UnsafeAdapter.UnsafeMemory</code></p>
     */
    public static class UnsafeMemory implements UnsafeMemoryMBean  {
    	
    	/** The map key for the total memory allocation in bytes */
    	public static final String ALLOC_MEM = "Memory";
    	/** The map key for the total memory alignedment overhead in bytes */
    	public static final String ALLOC_OVER = "AllocationOverhead";
    	
    	/** The map key for the total memory allocation in KB */
    	public static final String ALLOC_MEMK = "MemoryKb";
    	/** The map key for the total memory allocation in MB */
    	public static final String ALLOC_MEMM = "MemoryMb";
    	/** The map key for the total number of current allocations */
    	public static final String ALLOC_COUNT = "Allocations";
    	/** The map key for the reference queue size */
    	public static final String REFQ_SIZE= "RefQSize";
    	/** The map key for the pending phantom references */
    	public static final String PENDING_COUNT = "Pending";
    	
    	/**
    	 * {@inheritDoc}
    	 * @see org.helios.jmx.util.unsafe.UnsafeAdapter.UnsafeMemoryMBean#getState()
    	 */
    	@Override
    	public Map<String, Long> getState() {
    		Map<String, Long> map = new LinkedHashMap<String, Long>(10);
    		map.put(ALLOC_MEM, getTotalAllocatedMemory());
    		map.put(ALLOC_OVER, getAlignedMemoryOverhead());
    		map.put(ALLOC_MEMK, getTotalAllocatedMemoryKb());
    		map.put(ALLOC_MEMM, getTotalAllocatedMemoryMb());
    		map.put(ALLOC_COUNT, (long)getTotalAllocationCount());
    		map.put(PENDING_COUNT, (long)getPendingRefs());    		
    		map.put("BaselineMemory", BASELINE_MEM);
    		map.put("BaselineAllocations", (long)BASELINE_ALLOCS);
    		map.put("BaselinePending", (long)BASELINE_PENDING);
    		map.put("BaselineOverhead", BASELINE_AO);
    		
    		return map;
    	}
    	
		/**
		 * {@inheritDoc}
		 * @see org.helios.jmx.util.unsafe.UnsafeAdapter.UnsafeMemoryMBean#getAddressSize()
		 */
		@Override
		public int getAddressSize() {
			return addressSize();
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.jmx.util.unsafe.UnsafeAdapter.UnsafeMemoryMBean#getPageSize()
		 */
		@Override
		public int getPageSize() {
			return pageSize();
		}
    	
    	

		/**
		 * {@inheritDoc}
		 * @see org.helios.jmx.util.unsafe.UnsafeAdapter.UnsafeMemoryMBean#getTotalAllocatedMemory()
		 */
		@Override
		public long getTotalAllocatedMemory() {
			if(!trackMem) return -1L;
			return totalMemoryAllocated.get()-BASELINE_MEM;
		}
		
		/**
		 * {@inheritDoc}
		 * @see org.helios.jmx.util.unsafe.UnsafeAdapter.UnsafeMemoryMBean#getAlignedMemoryOverhead()
		 */
		public long getAlignedMemoryOverhead() {
			if(!trackMem) return -1L;
			return totalAlignmentOverhead.get()-BASELINE_AO;
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.jmx.util.unsafe.UnsafeAdapter.UnsafeMemoryMBean#getTotalAllocationCount()
		 */
		@Override
		public int getTotalAllocationCount() {
			if(!trackMem) return -1;
			return memoryAllocations.size()-BASELINE_ALLOCS;
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.jmx.util.unsafe.UnsafeAdapter.UnsafeMemoryMBean#getTotalAllocatedMemoryKb()
		 */
		@Override
		public long getTotalAllocatedMemoryKb() {
			if(!trackMem) return -1L;
			long t = totalMemoryAllocated.get();
			if(t<1) return 0L;
			return t/1024;
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.jmx.util.unsafe.UnsafeAdapter.UnsafeMemoryMBean#getTotalAllocatedMemoryMb()
		 */
		@Override
		public long getTotalAllocatedMemoryMb() {
			if(!trackMem) return -1L;
			long t = totalMemoryAllocated.get();
			if(t<1) return 0L;
			return t/1024/1024;
		}
		
    	/**
    	 * Returns the number of retained phantom references to memory allocations
    	 * @return the number of retained phantom references to memory allocations
    	 */
    	public int getPendingRefs() {
    		return deAllocs.size() - BASELINE_PENDING;
    	}
		
		
    }
    
    
    /**
     * <p>Title: MemoryAllocationReference</p>
     * <p>Description: A phantom reference extension for tracking memory allocations without preventing them from being enqueued for de-allocation</p> 
     * <p>Company: Helios Development Group LLC</p>
     * @author Whitehead (nwhitehead AT heliosdev DOT org)
     * <p><code>org.helios.jmx.util.unsafe.UnsafeAdapter.MemoryAllocationReference</code></p>
     */
    public static class MemoryAllocationReference extends PhantomReference<DeAllocateMe> implements ReferenceRunnable {
    	/** The index of this reference */
    	private final long index = refIndexFactory.incrementAndGet();
    	/** The memory addresses owned by this reference */
    	private final long[][] addresses;
    	/** Debug runnable */
    	private final Runnable runOnClear;
    	
    	private static final ReferenceQueue<DeAllocateMe> refQueue = ReferenceService.getInstance().getReferenceQueue(DeAllocateMe.class);
    	
		/**
		 * Creates a new MemoryAllocationReference
		 * @param referent the memory address holder
		 */
		public MemoryAllocationReference(final DeAllocateMe referent) {
			super(referent, refQueue);
			addresses = referent==null ? EMPTY_ADDRESSES : referent.getAddresses();
			deAllocs.put(index, this);
			
			if(LOG.isTraceEnabled()) {
				final String name = referent.getClass().getSimpleName();
				runOnClear = new Runnable() {
					public void run() {
//						final String refMsg = referent.getClass().getName() + " " + Arrays.toString(addresses);
//						LOG.trace("Deallocated instance of {}, Addr:{}", name, Arrays.toString(addresses));
					}
				};
			} else {
				runOnClear = null;
			}
			
		}    	
		
		/**
		 * {@inheritDoc}
		 * @see java.lang.ref.Reference#clear()
		 */
		@Override
		public void clear() {
			for(long[] address: addresses) {
				if(address[0]>0) {
					freeMemory(address[0]);
					address[0] = 0L;
					if(runOnClear!=null) runOnClear.run();
				}
				deAllocs.remove(index);
				
			}
			super.clear();
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {			
			clear();    
			if(runOnClear!=null) {
				runOnClear.run();
			}			
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.jmx.util.reference.ReferenceRunnable#getClearedRunnable()
		 */
		@Override
		public Runnable getClearedRunnable() {
			return this;
		}
		
    }
    
    /** Static class logger */
    private static final Logger LOG = LoggerFactory.getLogger(UnsafeAdapter.class);

    
//    /** The memory allocation de-allocator task */
//    private static final Runnable deallocator = new Runnable() {
//    	public void run() {
//    		latch.countDown();
//    		LOG.info(StringHelper.banner("Started Unsafe Memory Manager Thread"));
//    		while(true) {
//    			try {
//    				MemoryAllocationReference phantom = (MemoryAllocationReference) deallocations.remove();
//    				refQueueSize.decrementAndGet();
//    				phantom.clear();    				
//    			} catch (Throwable t) {
//    				if(Thread.interrupted()) Thread.interrupted();
//    			}
//    		}
//    	}
//    };
    
//    private static final CountDownLatch latch = new CountDownLatch(1);
    
    public static List<MemoryAllocationReference> registerForDeAlloc(DeAllocateMe...deallocators) {
//    	try {
//    		latch.await();
//    	} catch (Exception ex) {
//    		throw new RuntimeException(ex);
//    	}
    	if(deallocators==null || deallocators.length==0) return EMPTY_ALLOC_LIST;
    	List<MemoryAllocationReference> refs = new ArrayList<MemoryAllocationReference>();
    	for(DeAllocateMe dame: deallocators) {
    		if(dame==null) continue;
    		long[][] addresses = dame.getAddresses();
    		if(addresses==null || addresses.length==0) continue;
    		refs.add(new MemoryAllocationReference(dame));
    	}
    	if(refs.isEmpty()) return EMPTY_ALLOC_LIST;
    	if(LOG.isTraceEnabled()) {
    		final List<String> names = new ArrayList<String>(deallocators.length);
    		for(DeAllocateMe dame: deallocators) {
    			if(dame==null) continue;
    			names.add(dame.getClass().getName() + " " + Arrays.toString(dame.getAddresses()));
    		}
//    		LOG.trace("Created {} MemoryAllocationReferences: {}", refs.size(), names.toString());
    	}
    	return null; //Collections.unmodifiableList(refs);
    }
    
    
    
    

    static {
//    	Thread t = new Thread(deallocator, "UnsafeAdapterDeallocatorThread");
//    	t.setPriority(Thread.MAX_PRIORITY);
//    	t.setDaemon(true);
//    	t.start();

        try {        	
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (Unsafe) theUnsafe.get(null);
            ADDRESS_SIZE = UNSAFE.addressSize();
            BYTES_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);
            OBJECTS_OFFSET = UNSAFE.arrayBaseOffset(Object[].class);
            INT_ARRAY_OFFSET = UNSAFE.arrayBaseOffset(int[].class);
            LONG_ARRAY_OFFSET = UNSAFE.arrayBaseOffset(long[].class);
            DOUBLE_ARRAY_OFFSET = UNSAFE.arrayBaseOffset(double[].class);
            BYTE_ARRAY_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);
            CHAR_ARRAY_OFFSET = UNSAFE.arrayBaseOffset(char[].class);
            
            
            int copyMemCount = 0;
            int setMemCount = 0;

            for(Method method: Unsafe.class.getDeclaredMethods()) {
            	if("copyMemory".equals(method.getName())) {
            		copyMemCount++;
            	}
            	if("setMemory".equals(method.getName())) {
            		setMemCount++;
            	}
            }
            FIVE_COPY = copyMemCount>1;
            FOUR_SET = setMemCount>1;
        	trackMem = System.getProperties().containsKey(TRACK_ALLOCS_PROP) || ("false".equalsIgnoreCase(System.getProperty(TRACK_ALLOCS_PROP)) && isDebugAgentLoaded() );   
        	alignMem = System.getProperties().containsKey(ALIGN_ALLOCS_PROP);
        	if(trackMem) {
        		unsafeMemoryStats = new UnsafeMemory();
        		memoryAllocations = new NonBlockingHashMapLong<long[]>(1024, true);
        		totalMemoryAllocated = new AtomicLong(0L);
        		totalAlignmentOverhead = new AtomicLong(0L);
        		JMXHelper.registerMBean(unsafeMemoryStats, JMXHelper.objectName("%s:%s=%s", UnsafeAdapter.class.getPackage().getName(), "service", UnsafeMemory.class.getSimpleName()));
        	} else {
        		totalMemoryAllocated = null;
        		memoryAllocations = null;
        		totalAlignmentOverhead = null;
        		unsafeMemoryStats = new InactiveUnsafeMemory();
        	}
        	ReferenceService.getInstance();
        	if(trackMem) {
        		BASELINE_ALLOCS = unsafeMemoryStats.getTotalAllocationCount();
        		BASELINE_MEM = unsafeMemoryStats.getTotalAllocatedMemory();
        	    BASELINE_PENDING = unsafeMemoryStats.getPendingRefs();
        	    BASELINE_AO = unsafeMemoryStats.getAlignedMemoryOverhead();
        	} else {
        		BASELINE_ALLOCS = -1;
        		BASELINE_MEM = -1L;
        		BASELINE_PENDING = -1;
        		BASELINE_AO = -1;
        		
        	}
        	
        } catch (Exception e) {
            throw new AssertionError(e);
        } finally {
        }
    }
    
	/**
	 * Sets all bytes in a given block of memory to a copy of another block.
	 * Equivalent to {@code copyMemory(null, srcAddress, null, destAddress, bytes)}.
	 * @param srcAddress The address of the source
	 * @param targetAddress The address of the target
	 * @param numberOfBytes The number of bytes to copy
	 * @see sun.misc.Unsafe#copyMemory(long, long, long)
	 */
	public static void copyMemory(long srcAddress, long targetAddress, long numberOfBytes) {
		UNSAFE.copyMemory(srcAddress, targetAddress, numberOfBytes);
	}
	
    /**
     * Sets all bytes in a given block of memory to a copy of another block
     * @param srcBase The source object
     * @param srcOffset The source object offset
     * @param destBase The destination object
     * @param destOffset The destination object offset
     * @param bytes The byte count to copy
     */
    public static void copyMemory(Object srcBase, long srcOffset, Object destBase, long destOffset, long bytes) {
    	if(FIVE_COPY) {
    		UNSAFE.copyMemory(srcBase, srcOffset, destBase, destOffset, bytes);
    	} else {
    		UNSAFE.copyMemory(srcOffset + getAddressOf(srcBase), destOffset + getAddressOf(destBase), bytes);
    	}
    }    
    
    
    /**
     * Sets all bytes in a given block of memory to a fixed value
     * @param obj The target object
     * @param offset  The target object offset
     * @param bytes The numer of bytes to set
     * @param value The value to set the bytes to
     */
    public static void setMemory(Object obj, long offset, long bytes, byte value) {
    	if(FOUR_SET) {
    		UNSAFE.setMemory(obj, offset, bytes, value);
    	} else {
    		UNSAFE.setMemory(offset + getAddressOf(obj), bytes, value);
    	}
    }
    

	/**
	 * Sets all bytes in a given block of memory to a fixed value (usually zero). This provides a single-register addressing mode, as discussed in {@link #getInt(Object,long)} 
	 * @param address The starting address of the segment to write to
	 * @param bytes The number of bytes in the segment 
	 * @param value The value to write to each byte in the segment 
	 * @see sun.misc.Unsafe#setMemory(long, long, byte)
	 */
	public static void setMemory(long address, long bytes, byte value) {
		UNSAFE.setMemory(address, bytes, value);
	}
	
    
    
    /**
     * Returns a map of unsafe memory stats keyed by the stat name
     * @return a map of unsafe memory stats 
     */
    public static Map<String, Long> getUnsafeMemoryStats() {
    	return trackMem ? unsafeMemoryStats.getState() : EMPTY_STAT_MAP;
    }
    
    /**
     * Returns a string of the printed memory stats
     * @return a string of the printed memory stats
     */
    public static String printUnsafeMemoryStats() {
    	if(!trackMem) return "";
    	StringBuilder b = new StringBuilder("\n\t================================\n\tUnsafe Memory Stats\n\t================================");
    	for(Map.Entry<String, Long> entry: getUnsafeMemoryStats().entrySet()) {
    		b.append("\n\t").append(entry.getKey()).append(" : ").append(entry.getValue());
    	}
    	b.append("\n\t================================\n");
    	return b.toString();
    }
    
    
    /**
     * Returns the address of the passed object
     * @param obj The object to get the address of 
     * @return the address of the passed object or zero if the passed object is null
     */
    public static long getAddressOf(Object obj) {
    	if(obj==null) return 0;
    	Object[] array = new Object[] {obj};
    	return ADDRESS_SIZE==4 ? UNSAFE.getInt(array, OBJECTS_OFFSET) : UNSAFE.getLong(array, OBJECTS_OFFSET);
    }

	/**
	 * @return
	 * @see sun.misc.Unsafe#addressSize()
	 */
	public static int addressSize() {
		return UNSAFE.addressSize();
	}

	/**
	 * Creates an instance of a class
	 * @param clazz The class to allocate
	 * @return the instantiated object
	 * @throws InstantiationException
	 * @see sun.misc.Unsafe#allocateInstance(java.lang.Class)
	 */
	public static Object allocateInstance(Class<?> clazz) throws InstantiationException {
		return UNSAFE.allocateInstance(clazz);
	}
	
	/**
	 * Returns the size of the memory chunk allocated at the specified address.
	 * Only returns the correct value if the management interface is enabled.
	 * Otherwise returns -1.
	 * @param address The address to get the size of 
	 * @return The size of the memory allocation in bytes
	 */
	public static long sizeOf(long address) {
		if(trackMem) {
			long[] alloc = memoryAllocations.get(address);
			return alloc!=null ? alloc[0] : -1L;
		}
		return -1L;
	}
	
	/**
	 * Allocates a chunk of memory and returns its address
	 * @param size The number of bytes to allocate
	 * @param alignmentOverhead The number of bytes allocated in excess of requested for alignment
	 * @return The address of the allocated memory
	 * @see sun.misc.Unsafe#allocateMemory(long)
	 */
	private static long _allocateMemory(long size, long alignmentOverhead) {
		long address = UNSAFE.allocateMemory(size);
		if(trackMem) {		
			memoryAllocations.put(address, new long[]{size, alignmentOverhead});
			totalMemoryAllocated.addAndGet(size);
			totalAlignmentOverhead.addAndGet(alignmentOverhead);
		}
		return address;
	}
	
	/**
	 * Resizes a new block of native memory, to the given size in bytes. 
	 * @param The address of the existing allocation
	 * @param bytes The size of the new allocation i n bytes
	 * @return The address of the new allocation
	 * @see sun.misc.Unsafe#reallocateMemory(long, long)
	 */
	public static long _reallocateMemory(long address, long size, long alignmentOverhead) {
		long newAddress = UNSAFE.reallocateMemory(address, size);
		if(trackMem) {
			// ==========================================================
			//  Subtract pervious allocation
			// ==========================================================				
			long[] alloc = memoryAllocations.remove(address);
			if(alloc!=null) {
				totalMemoryAllocated.addAndGet(-1L * alloc[0]);
				totalAlignmentOverhead.addAndGet(-1L * alloc[1]);
			}
			// ==========================================================
			//  Add new allocation
			// ==========================================================								
			memoryAllocations.put(newAddress, new long[]{size, alignmentOverhead});
			totalMemoryAllocated.addAndGet(size);
			totalAlignmentOverhead.addAndGet(alignmentOverhead);
		}
		return newAddress;

	}

	/**
	 * Allocates a chunk of memory and returns its address
	 * @param size The number of bytes to allocate
	 * @return The address of the allocated memory
	 * @see sun.misc.Unsafe#allocateMemory(long)
	 */
	public static long allocateMemory(long size) {
		return _allocateMemory(size, 0);
	}
	
	/**
	 * Allocates a chunk of memory and returns its address
	 * @param size The number of bytes to allocate
	 * @param dealloc The deallocatable to register for deallocation when it becomes phantom reachable
	 * @return The address of the allocated memory
	 * @see sun.misc.Unsafe#allocateMemory(long)
	 */
	public static long allocateMemory(long size, DeAllocateMe dealloc) {
		registerForDeAlloc(dealloc);
		return _allocateMemory(size, 0);
	}
	
	/**
	 * Allocates a chunk of aligned (if enabled) memory and returns its address
	 * @param size The number of bytes to allocate
	 * @param dealloc The optional deallocatable to register for deallocation when it becomes phantom reachable
	 * @return The address of the allocated memory
	 * @see sun.misc.Unsafe#allocateMemory(long)
	 */
	public static long allocateAlignedMemory(long size, DeAllocateMe dealloc) {
		if(dealloc!=null) {
			registerForDeAlloc(dealloc);
		}
		if(alignMem && size <= MAX_ALIGNED_MEM) {
			int actual = findNextPositivePowerOfTwo((int)size);
			return _allocateMemory(actual, actual-size);
		} 
		return _allocateMemory(size, 0);
	}
	
	/**
	 * Allocates a chunk of aligned (if enabled) memory and returns its address
	 * @param size The number of bytes to allocate
	 * @return The address of the allocated memory
	 * @see sun.misc.Unsafe#allocateMemory(long)
	 */
	public static long allocateAlignedMemory(long size) {
		return allocateAlignedMemory(size, null);
	}
	
	
	/**
	 * Resizes a new block of native memory, to the given size in bytes. 
	 * @param The address of the existing allocation
	 * @param bytes The size of the new allocation in bytes
	 * @return The address of the new allocation
	 * @see sun.misc.Unsafe#reallocateMemory(long, long)
	 */
	public static long reallocateMemory(long address, long bytes) {
		return _reallocateMemory(address, bytes, 0);
	}	
	
	/**
	 * Resizes a new block of aligned (if enabled) native memory, to the given size in bytes. 
	 * @param The address of the existing allocation
	 * @param bytes The size of the new allocation i n bytes
	 * @return The address of the new allocation
	 * @see sun.misc.Unsafe#reallocateMemory(long, long)
	 */
	public static long reallocateAlignedMemory(long address, long size) {
		if(alignMem && size <= MAX_ALIGNED_MEM) {
			int actual = findNextPositivePowerOfTwo((int)size);
			return _reallocateMemory(address, actual, actual-size);
		} 
		return _reallocateMemory(address, size, 0);
	}	
	
	/**
	 * Frees the memory allocated at the passed address
	 * @param address The address of the memory to free
	 * @see sun.misc.Unsafe#freeMemory(long)
	 */
	public static void freeMemory(long address) {
		if(trackMem) {
			// ==========================================================
			//  Subtract pervious allocation
			// ==========================================================				
			long[] alloc = memoryAllocations.remove(address);
			if(alloc!=null) {				
				totalMemoryAllocated.addAndGet(-1L * alloc[0]);
				totalAlignmentOverhead.addAndGet(-1L * alloc[1]);
			}
		}		
		UNSAFE.freeMemory(address);
	}
	

	

	/**
	 * Report the offset of the first element in the storage allocation of a 
	 * given array class.  If #arrayIndexScale  returns a non-zero value 
	 * for the same class, you may use that scale factor, together with this 
	 * base offset, to form new offsets to access elements of arrays of the given class.
	 * @param clazz The component type of an array class
	 * @return the base offset
	 * @see sun.misc.Unsafe#arrayBaseOffset(java.lang.Class)
	 */
	public static int arrayBaseOffset(Class<?> clazz) {
		return UNSAFE.arrayBaseOffset(clazz);
	}

	/**
	 * Report the scale factor for addressing elements in the storage allocation of a given array class.  
	 * However, arrays of "narrow" types 
	 * will generally not work properly with accessors like #getByte(Object, int) , 
	 * so the scale factor for such classes is reported as zero.
	 * @param clazz
	 * @return the index scale
	 * @see sun.misc.Unsafe#arrayIndexScale(java.lang.Class)
	 */
	public static int arrayIndexScale(Class<?> clazz) {
		return UNSAFE.arrayIndexScale(clazz);
	}

	/**
	 * Atomically update Java variable or address to x if it is currently holding expected.
	 * @param object The object within which the variable is to be updated. Null if updating pure address.
	 * @param offset The offset from the base address at which the variable is to be updated, or the address to update the value at (if object is null) 
	 * @param expect The expected value to find in the variable or at the address
	 * @param value The value to set into the variable or address
	 * @return true if succeeded, false otherwise
	 * @see sun.misc.Unsafe#compareAndSwapInt(java.lang.Object, long, int, int)
	 */
	public static final boolean compareAndSwapInt(Object object, long offset, int expect, int value) {
		return UNSAFE.compareAndSwapInt(object, offset, expect, value);
	}
	
	/**
	 * Atomically increments the int at the passed address and returns the incremented value
	 * @param offset The address of the int to increment and return
	 * @return the incremented value
	 */
	public static final int incrementIntAndGet(long offset) {
		for (;;) {
            int current = getInt(offset);
            int next = current + 1;
            if (compareAndSwapInt(null, offset, current, next))
                return next;
        }		
	}
	
	/**
	 * Atomically decrements the int at the passed address and returns the decremented value
	 * @param offset The address of the int to decrement and return
	 * @return the decremented value
	 */
	public final int decrementIntAndGet(long offset) {
        for (;;) {
            int current = getInt(offset);
            int next = current - 1;
            if (compareAndSwapInt(null, offset, current, next))
                return next;
        }
	}	

	

	/**
	 * Atomically update Java variable or address to x if it is currently holding expected.
	 * @param object The object within which the variable is to be updated. Null if updating pure address.
	 * @param offset The offset from the base address at which the variable is to be updated, or the address to update the value at (if object is null) 
	 * @param expect The expected value to find in the variable or at the address
	 * @param value The value to set into the variable or address
	 * @return true if succeeded, false otherwise
	 * @see sun.misc.Unsafe#compareAndSwapLong(java.lang.Object, long, long, long)
	 */
	public static final boolean compareAndSwapLong(Object object, long offset, long expect, long value) {
		return UNSAFE.compareAndSwapLong(object, offset, expect, value);
	}
	
	/**
	 * Atomically update Java variable or address to x if it is currently holding any of the expecteds.
	 * @param object The object within which the variable is to be updated. Null if updating pure address.
	 * @param offset The offset from the base address at which the variable is to be updated, or the address to update the value at (if object is null) 
	 * @param expects The expected values to find in the variable or at the address
	 * @param value The value to set into the variable or address
	 * @return true if succeeded, false otherwise
	 * @see sun.misc.Unsafe#compareAndSwapLong(java.lang.Object, long, long, long)
	 */
	public static final boolean compareMultiAndSwapLong(Object object, long offset, long value, long...expects) {
		if(expects==null || expects.length==0) return false;
		for(long expect: expects) {
			if(UNSAFE.compareAndSwapLong(object, offset, expect, value)) return true;
		}
		return false;
	}


	/**
	 * Atomically update Java variable or address to x if it is currently holding expected.
	 * @param object The object within which the variable is to be updated. Null if updating pure address.
	 * @param offset The offset from the base address at which the variable is to be updated, or the address to update the value at (if object is null) 
	 * @param expect The expected value to find in the variable or at the address
	 * @param value The value to set into the variable or address
	 * @return true if succeeded, false otherwise
	 * @see sun.misc.Unsafe#compareAndSwapObject(java.lang.Object, long, java.lang.Object, java.lang.Object)
	 */
	public static final boolean compareAndSwapObject(Object object, long offset, Object expect, Object value) {
		return UNSAFE.compareAndSwapObject(object, offset, expect, value);
	}

	
    
    /**
     * Finds the next <b><code>power of 2</code></b> higher or equal to than the passed value.
     * @param value The initial value
     * @return the pow2
     */
    public static int findNextPositivePowerOfTwo(final int value) {
    	return  1 << (32 - Integer.numberOfLeadingZeros(value - 1));
	}    
    
    /**
     * Finds the next <b><code>power of 2</code></b> higher or equal to than the passed value.
     * @param value The initial value
     * @return the pow2
     */
    public static int findNextPositivePowerOfTwo(final long value) {
    	return  1 << (64 - Long.numberOfLeadingZeros(value - 1));
	}    

	/**
	 * Define a class but do not make it known to the class loader or system dictionary.
	 * @param hostClass the host class to define the anonymous class in
	 * @param byte[] byteCode The byte code for the class to create
	 * @param cpPatches An array of constant pool patches
	 * @return the defined anonymous class
	 * @see sun.misc.Unsafe#defineAnonymousClass(java.lang.Class, byte[], java.lang.Object[])
	 */
	public static Class<?> defineAnonymousClass(Class<?> hostClass, byte[] byteCode, Object... cpPatches) {
		return UNSAFE.defineAnonymousClass(hostClass, byteCode, cpPatches);
	}
 
	/**
	 * Tell the VM to define a class, without security checks.  By default, the 
	 * class loader and protection domain come from the caller's class.
	 * @param className The name of the class to define 
	 * @param byteCode The byte code for the class to define
	 * @param offset The offset in the byte code array to start at
	 * @param length The length of the byte code in the byte code array
	 * @param classLoader the classloader to load the class with
	 * @param The protection domain of the defined class  
	 * @return the defined class
	 * @see sun.misc.Unsafe#defineClass(java.lang.String, byte[], int, int, java.lang.ClassLoader, java.security.ProtectionDomain)
	 */
	public static Class<?> defineClass(String className, byte[] byteCode, int offset, int length,
			ClassLoader classLoader, ProtectionDomain protectionDomain) {
		return UNSAFE.defineClass(className, byteCode, offset, length, classLoader, protectionDomain);
	}

	/**
	 * Tell the VM to define a class, without security checks.  By default, the 
	 * class loader and protection domain come from the caller's class.
	 * @param className The name of the class to define 
	 * @param byteCode The byte code for the class to define
	 * @param offset The offset in the byte code array to start at
	 * @param length The length of the byte code in the byte code array
	 * @return the defined class
	 * @deprecated
	 * @see sun.misc.Unsafe#defineClass(java.lang.String, byte[], int, int)
	 */
	public static Class<?> defineClass(String className, byte[] byteCode, int offset, int length) {
		return UNSAFE.defineClass(className, byteCode, offset, length);
	}

	/**
	 * Ensure the given class has been initialized. This is often needed in conjunction 
	 * with obtaining the static field base of a class.
	 * @param clazz The class to ensure initialization for
	 * @see sun.misc.Unsafe#ensureClassInitialized(java.lang.Class)
	 */
	public static void ensureClassInitialized(Class<?> clazz) {
		UNSAFE.ensureClassInitialized(clazz);
	}

	/**
	 * Returns the offset of a field, truncated to 32 bits.
	 * Deprecated! As - of 1.4.1, use {@link #staticFieldOffset} for static fields and {@link #objectFieldOffset} for non-static fields.
	 * @param field The field to get the offset of
	 * @return the field offset
	 * @deprecated
	 * @see sun.misc.Unsafe#fieldOffset(java.lang.reflect.Field)
	 */
	public static int fieldOffset(Field field) {
		return UNSAFE.fieldOffset(field);
	}


	/**
	 * Fetches a native pointer from a given memory address.  If the address is 
	 * zero, or does not point into a block obtained from {@link #allocateMemory}, the results are undefined. 
	 * @param address The address to read from
	 * @return the returned address
	 * @see sun.misc.Unsafe#getAddress(long)
	 */
	public static long getAddress(long address) {
		return UNSAFE.getAddress(address);
	}

	/**
	 * Reads a boolean from an object at the specified offset.
	 * Deprecated! As - of 1.4.1, cast the 32-bit offset argument to a long. See {@link #staticFieldOffset}.
	 * @param Object object
	 * @param offset the offset of the field in the object
	 * @return the read boolean value
	 * @deprecated 
	 * @see sun.misc.Unsafe#getBoolean(java.lang.Object, int)
	 */
	public static boolean getBoolean(Object object, int offset) {
		return UNSAFE.getBoolean(object, offset);
	}
	
	/**
	 * Reads and returns a boolean from the specified address
	 * @param address The address where the boolean will be read from
	 * @return the read boolean value
	 */
	public static boolean getBoolean(long address) {
		return UNSAFE.getBoolean(null, address);
	}	

	/**
	 * Reads a boolean from an object at the specified offset.
	 * @param Object object
	 * @param offset the offset of the field in the object
	 * @return the read boolean value
	 * @see sun.misc.Unsafe#getBoolean(java.lang.Object, long)
	 */
	public static boolean getBoolean(Object object, long offset) {
		return UNSAFE.getBoolean(object, offset);
	}

	/**
	 * Volatile version of {@link #getBoolean(Object, long)} 
	 * @param Object object
	 * @param offset the offset of the field in the object
	 * @return the read boolean value
	 * @see sun.misc.Unsafe#getBooleanVolatile(java.lang.Object, long)
	 */
	public static boolean getBooleanVolatile(Object object, long offset) {
		return UNSAFE.getBooleanVolatile(object, offset);
	}

	/**
	 * @param arg0
	 * @return
	 * @see sun.misc.Unsafe#getByte(long)
	 */
	public static byte getByte(long arg0) {
		return UNSAFE.getByte(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @deprecated
	 * @see sun.misc.Unsafe#getByte(java.lang.Object, int)
	 */
	public static byte getByte(Object arg0, int arg1) {
		return UNSAFE.getByte(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see sun.misc.Unsafe#getByte(java.lang.Object, long)
	 */
	public static byte getByte(Object arg0, long arg1) {
		return UNSAFE.getByte(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see sun.misc.Unsafe#getByteVolatile(java.lang.Object, long)
	 */
	public static byte getByteVolatile(Object arg0, long arg1) {
		return UNSAFE.getByteVolatile(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @return
	 * @see sun.misc.Unsafe#getChar(long)
	 */
	public static char getChar(long arg0) {
		return UNSAFE.getChar(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @deprecated
	 * @see sun.misc.Unsafe#getChar(java.lang.Object, int)
	 */
	public static char getChar(Object arg0, int arg1) {
		return UNSAFE.getChar(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see sun.misc.Unsafe#getChar(java.lang.Object, long)
	 */
	public static char getChar(Object arg0, long arg1) {
		return UNSAFE.getChar(arg0, arg1);
	}
	
	/**
	 * Reads a series of chars starting at the passed address and returns them as an array
	 * @param address The address to read from
	 * @param size The number of chars to read
	 * @return the read chars as an array
	 */
	public static char[] getCharArray(long address, int size) {
		char[] arr = new char[size];
		copyMemory(null, address, arr, CHAR_ARRAY_OFFSET, CHAR_SIZE * size);
		return arr;
	}	
	

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see sun.misc.Unsafe#getCharVolatile(java.lang.Object, long)
	 */
	public static char getCharVolatile(Object arg0, long arg1) {
		return UNSAFE.getCharVolatile(arg0, arg1);
	}

	/**
	 * Retrieves the value at the given address
	 * @param address The address to read the value from
	 * @return the read value
	 * @see sun.misc.Unsafe#getDouble(long)
	 */
	public static double getDouble(long address) {
		return UNSAFE.getDouble(address);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @deprecated
	 * @see sun.misc.Unsafe#getDouble(java.lang.Object, int)
	 */
	public static double getDouble(Object arg0, int arg1) {
		return UNSAFE.getDouble(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see sun.misc.Unsafe#getDouble(java.lang.Object, long)
	 */
	public static double getDouble(Object arg0, long arg1) {
		return UNSAFE.getDouble(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see sun.misc.Unsafe#getDoubleVolatile(java.lang.Object, long)
	 */
	public static double getDoubleVolatile(Object arg0, long arg1) {
		return UNSAFE.getDoubleVolatile(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @return
	 * @see sun.misc.Unsafe#getFloat(long)
	 */
	public static float getFloat(long arg0) {
		return UNSAFE.getFloat(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @deprecated
	 * @see sun.misc.Unsafe#getFloat(java.lang.Object, int)
	 */
	public static float getFloat(Object arg0, int arg1) {
		return UNSAFE.getFloat(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see sun.misc.Unsafe#getFloat(java.lang.Object, long)
	 */
	public static float getFloat(Object arg0, long arg1) {
		return UNSAFE.getFloat(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see sun.misc.Unsafe#getFloatVolatile(java.lang.Object, long)
	 */
	public static float getFloatVolatile(Object arg0, long arg1) {
		return UNSAFE.getFloatVolatile(arg0, arg1);
	}

	/**
	 * Returns the int value at the specified address
	 * @param address The address of the int to read
	 * @return the read int
	 * @see sun.misc.Unsafe#getInt(long)
	 */
	public static int getInt(long address) {
		return UNSAFE.getInt(address);
	}

	/**
	 * Returns the int value at the specified offset of a non-null object, or the absolute address where the object is null.
	 * @param o The object from which to read the value, or null if the address is absolute 
	 * @param address The address of the int to read
	 * @return the read value
	 * @deprecated
	 * @see sun.misc.Unsafe#getInt(java.lang.Object, int)
	 */
	public static int getInt(Object o, int address) {
		return UNSAFE.getInt(o, address);
	}

	/**
	 * Returns the int value at the specified offset of a non-null object, or the absolute address where the object is null.
	 * @param o The object from which to read the value, or null if the address is absolute 
	 * @param address The address of the int to read
	 * @return the read value
	 * @see sun.misc.Unsafe#getInt(java.lang.Object, long)
	 */
	public static int getInt(Object o, long address) {
		return UNSAFE.getInt(o, address);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see sun.misc.Unsafe#getIntVolatile(java.lang.Object, long)
	 */
	public static int getIntVolatile(Object arg0, long arg1) {
		return UNSAFE.getIntVolatile(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see sun.misc.Unsafe#getLoadAverage(double[], int)
	 */
	public static int getLoadAverage(double[] arg0, int arg1) {
		return UNSAFE.getLoadAverage(arg0, arg1);
	}

	/**
	 * Returns the long at the passed address
	 * @param address The address to read from
	 * @return the long value
	 * @see sun.misc.Unsafe#getLong(long)
	 */
	public static long getLong(long address) {
		return UNSAFE.getLong(address);
	}
	
	/**
	 * Reads a series of bytes starting at the passed address and returns them as an array
	 * @param address The address to read from
	 * @param size The number of bytes to read
	 * @return the read bytes as an array
	 */
	public static byte[] getByteArray(long address, int size) {
		byte[] arr = new byte[size];
		copyMemory(null, address, arr, BYTE_ARRAY_OFFSET, size);
		return arr;
	}
	
	
	/**
	 * Reads a series of longs starting at the passed address and returns them as an array
	 * @param address The address to read from
	 * @param size The number of longs to read
	 * @return the read longs as an array
	 */
	public static long[] getLongArray(long address, int size) {
		long[] arr = new long[size];
		copyMemory(null, address, arr, LONG_ARRAY_OFFSET, size << 3);
		return arr;
	}
	
	/**
	 * Reads a series of longs starting at the passed address and returns them as an array
	 * @param address The address to read from
	 * @param size The number of longs to read
	 * @return the read longs as an array
	 */
	public static double[] getDoubleArray(long address, int size) {
		double[] arr = new double[size];
		copyMemory(null, address, arr, DOUBLE_ARRAY_OFFSET, size << 3);
		return arr;
	}
	
	
	/**
	 * Writes a long array to the specified address
	 * @param address The address to write to
	 * @param values The long array to write
	 */
	public static void putLongArray(long address, long[] values) {
		if(values==null || values.length==0) return;
		copyMemory(values, LONG_ARRAY_OFFSET, null, address, values.length << 3);
	}
	
	/**
	 * Writes an int array to the specified address
	 * @param address The address to write to
	 * @param values The int array to write
	 */
	public static void putIntArray(long address, int[] values) {
		if(values==null || values.length==0) return;
		copyMemory(values, INT_ARRAY_OFFSET, null, address, values.length << 2);
	}	
	
	/**
	 * Reads a series of ints starting at the passed address and returns them as an array
	 * @param address The address to read from
	 * @param size The number of ints to read
	 * @return the read ints as an array
	 */
	public static int[] getIntArray(long address, int size) {
		int[] arr = new int[size];
		copyMemory(null, address, arr, INT_ARRAY_OFFSET, size << 2);
		return arr;
	}
	
	

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @deprecated
	 * @see sun.misc.Unsafe#getLong(java.lang.Object, int)
	 */
	public static long getLong(Object arg0, int arg1) {
		return UNSAFE.getLong(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see sun.misc.Unsafe#getLong(java.lang.Object, long)
	 */
	public static long getLong(Object arg0, long arg1) {
		return UNSAFE.getLong(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see sun.misc.Unsafe#getLongVolatile(java.lang.Object, long)
	 */
	public static long getLongVolatile(Object arg0, long arg1) {
		return UNSAFE.getLongVolatile(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @deprecated
	 * @see sun.misc.Unsafe#getObject(java.lang.Object, int)
	 */
	public static Object getObject(Object arg0, int arg1) {
		return UNSAFE.getObject(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see sun.misc.Unsafe#getObject(java.lang.Object, long)
	 */
	public static Object getObject(Object arg0, long arg1) {
		return UNSAFE.getObject(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see sun.misc.Unsafe#getObjectVolatile(java.lang.Object, long)
	 */
	public static Object getObjectVolatile(Object arg0, long arg1) {
		return UNSAFE.getObjectVolatile(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @return
	 * @see sun.misc.Unsafe#getShort(long)
	 */
	public static short getShort(long arg0) {
		return UNSAFE.getShort(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @deprecated
	 * @see sun.misc.Unsafe#getShort(java.lang.Object, int)
	 */	
	public static short getShort(Object arg0, int arg1) {
		return UNSAFE.getShort(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see sun.misc.Unsafe#getShort(java.lang.Object, long)
	 */
	public static short getShort(Object arg0, long arg1) {
		return UNSAFE.getShort(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see sun.misc.Unsafe#getShortVolatile(java.lang.Object, long)
	 */
	public static short getShortVolatile(Object arg0, long arg1) {
		return UNSAFE.getShortVolatile(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @see sun.misc.Unsafe#monitorEnter(java.lang.Object)
	 */
	public static void monitorEnter(Object arg0) {
		UNSAFE.monitorEnter(arg0);
	}

	/**
	 * @param arg0
	 * @see sun.misc.Unsafe#monitorExit(java.lang.Object)
	 */
	public static void monitorExit(Object arg0) {
		UNSAFE.monitorExit(arg0);
	}

	/**
	 * @param arg0
	 * @return
	 * @see sun.misc.Unsafe#objectFieldOffset(java.lang.reflect.Field)
	 */
	public static long objectFieldOffset(Field arg0) {
		return UNSAFE.objectFieldOffset(arg0);
	}

	/**
	 * @return
	 * @see sun.misc.Unsafe#pageSize()
	 */
	public static int pageSize() {
		return UNSAFE.pageSize();
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @see sun.misc.Unsafe#park(boolean, long)
	 */
	public static void park(boolean arg0, long arg1) {
		UNSAFE.park(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @see sun.misc.Unsafe#putAddress(long, long)
	 */
	public static void putAddress(long arg0, long arg1) {
		UNSAFE.putAddress(arg0, arg1);
	}

	/**
	 * Writes a boolean value to the offset address of the passed object, or if the object is null, 
	 * to the passed absolute address
	 * @param object The optional object to write to
	 * @param offset The offset off the address of the object to write to, 
	 * or an absolute address if the object is null
	 * @param bool The value to write
	 * @deprecated
	 */
	public static void putBoolean(Object object, int offset, boolean bool) {
		UNSAFE.putBoolean(object, offset, bool);
	}

	
	/**
	 * Writes a boolean value to the offset address of the passed object, or if the object is null, 
	 * to the passed absolute address
	 * @param object The optional object to write to
	 * @param offset The offset off the address of the object to write to, 
	 * or an absolute address if the object is null
	 * @param bool The value to write
	 */
	public static void putBoolean(Object object, long offset, boolean bool) {
		UNSAFE.putBoolean(object, offset, bool);
	}
	
	/**
	 * Writes a boolean to the passed address
	 * @param address The address to write to
	 * @param bool The boolean value to write
	 */
	public static void putBoolean(long address, boolean bool) {
		UNSAFE.putBoolean(null, address, bool);
		
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putBooleanVolatile(java.lang.Object, long, boolean)
	 */
	public static void putBooleanVolatile(Object arg0, long arg1, boolean arg2) {
		UNSAFE.putBooleanVolatile(arg0, arg1, arg2);
	}

	/**
	 * Sets the byte value at the specified address
	 * @param address The address of the target put
	 * @param value The value to put
	 * @see sun.misc.Unsafe#putByte(long, byte)
	 */
	public static void putByte(long address, byte value) {
		UNSAFE.putByte(address, value);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @deprecated
	 * @see sun.misc.Unsafe#putByte(java.lang.Object, int, byte)
	 */
	public static void putByte(Object arg0, int arg1, byte arg2) {
		UNSAFE.putByte(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putByte(java.lang.Object, long, byte)
	 */
	public static void putByte(Object arg0, long arg1, byte arg2) {
		UNSAFE.putByte(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putByteVolatile(java.lang.Object, long, byte)
	 */
	public static void putByteVolatile(Object arg0, long arg1, byte arg2) {
		UNSAFE.putByteVolatile(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @see sun.misc.Unsafe#putChar(long, char)
	 */
	public static void putChar(long arg0, char arg1) {
		UNSAFE.putChar(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @deprecated
	 * @see sun.misc.Unsafe#putChar(java.lang.Object, int, char)
	 */
	public static void putChar(Object arg0, int arg1, char arg2) {
		UNSAFE.putChar(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putChar(java.lang.Object, long, char)
	 */
	public static void putChar(Object arg0, long arg1, char arg2) {
		UNSAFE.putChar(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putCharVolatile(java.lang.Object, long, char)
	 */
	public static void putCharVolatile(Object arg0, long arg1, char arg2) {
		UNSAFE.putCharVolatile(arg0, arg1, arg2);
	}

	/**
	 * Sets the value at the passed address
	 * @param address The address to set the value at
	 * @param value The value to set
	 * @see sun.misc.Unsafe#putDouble(long, double)
	 */
	public static void putDouble(long address, double value) {
		UNSAFE.putDouble(address, value);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @deprecated
	 * @see sun.misc.Unsafe#putDouble(java.lang.Object, int, double)
	 */
	public static void putDouble(Object arg0, int arg1, double arg2) {
		UNSAFE.putDouble(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putDouble(java.lang.Object, long, double)
	 */
	public static void putDouble(Object arg0, long arg1, double arg2) {
		UNSAFE.putDouble(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putDoubleVolatile(java.lang.Object, long, double)
	 */
	public static void putDoubleVolatile(Object arg0, long arg1, double arg2) {
		UNSAFE.putDoubleVolatile(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @see sun.misc.Unsafe#putFloat(long, float)
	 */
	public static void putFloat(long arg0, float arg1) {
		UNSAFE.putFloat(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @deprecated
	 * @see sun.misc.Unsafe#putFloat(java.lang.Object, int, float)
	 */
	public static void putFloat(Object arg0, int arg1, float arg2) {
		UNSAFE.putFloat(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putFloat(java.lang.Object, long, float)
	 */
	public static void putFloat(Object arg0, long arg1, float arg2) {
		UNSAFE.putFloat(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putFloatVolatile(java.lang.Object, long, float)
	 */
	public static void putFloatVolatile(Object arg0, long arg1, float arg2) {
		UNSAFE.putFloatVolatile(arg0, arg1, arg2);
	}

	/**
	 * Sets the int value at the specified address
	 * @param address The address of the target put
	 * @param value The value to put
	 * @see sun.misc.Unsafe#putInt(long, int)
	 */
	public static void putInt(long address, int value) {
		UNSAFE.putInt(address, value);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @deprecated
	 * @see sun.misc.Unsafe#putInt(java.lang.Object, int, int)
	 */
	public static void putInt(Object arg0, int arg1, int arg2) {
		UNSAFE.putInt(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putInt(java.lang.Object, long, int)
	 */
	public static void putInt(Object arg0, long arg1, int arg2) {
		UNSAFE.putInt(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putIntVolatile(java.lang.Object, long, int)
	 */
	public static void putIntVolatile(Object arg0, long arg1, int arg2) {
		UNSAFE.putIntVolatile(arg0, arg1, arg2);
	}

	/**
	 * Sets the value at the passed address
	 * @param address The address to set the value at
	 * @param value The value to set
	 * @see sun.misc.Unsafe#putLong(long, long)
	 */
	public static void putLong(long address, long value) {
		UNSAFE.putLong(address, value);
	}
	
	/**
	 * Sets the long values in the array starting at at the specified address
	 * @param address The address of the target put
	 * @param values The values to put
	 * @see sun.misc.Unsafe#putLong(long, long)
	 */
	public static void putLongs(long address, long[] values) {
		copyMemory(values, LONG_ARRAY_OFFSET, null, address, values.length*LONG_SIZE);		
	}
	

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @deprecated
	 * @see sun.misc.Unsafe#putLong(java.lang.Object, int, long)
	 */
	public static void putLong(Object arg0, int arg1, long arg2) {
		UNSAFE.putLong(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putLong(java.lang.Object, long, long)
	 */
	public static void putLong(Object arg0, long arg1, long arg2) {
		UNSAFE.putLong(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putLongVolatile(java.lang.Object, long, long)
	 */
	public static void putLongVolatile(Object arg0, long arg1, long arg2) {
		UNSAFE.putLongVolatile(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @deprecated
	 * @see sun.misc.Unsafe#putObject(java.lang.Object, int, java.lang.Object)
	 */
	public static void putObject(Object arg0, int arg1, Object arg2) {
		UNSAFE.putObject(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putObject(java.lang.Object, long, java.lang.Object)
	 */
	public static void putObject(Object arg0, long arg1, Object arg2) {
		UNSAFE.putObject(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putObjectVolatile(java.lang.Object, long, java.lang.Object)
	 */
	public static void putObjectVolatile(Object arg0, long arg1, Object arg2) {
		UNSAFE.putObjectVolatile(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putOrderedInt(java.lang.Object, long, int)
	 */
	public static void putOrderedInt(Object arg0, long arg1, int arg2) {
		UNSAFE.putOrderedInt(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putOrderedLong(java.lang.Object, long, long)
	 */
	public static void putOrderedLong(Object arg0, long arg1, long arg2) {
		UNSAFE.putOrderedLong(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putOrderedObject(java.lang.Object, long, java.lang.Object)
	 */
	public static void putOrderedObject(Object arg0, long arg1, Object arg2) {
		UNSAFE.putOrderedObject(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @see sun.misc.Unsafe#putShort(long, short)
	 */
	public static void putShort(long arg0, short arg1) {
		UNSAFE.putShort(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @deprecated
	 * @see sun.misc.Unsafe#putShort(java.lang.Object, int, short)
	 */
	public static void putShort(Object arg0, int arg1, short arg2) {
		UNSAFE.putShort(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putShort(java.lang.Object, long, short)
	 */
	public static void putShort(Object arg0, long arg1, short arg2) {
		UNSAFE.putShort(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putShortVolatile(java.lang.Object, long, short)
	 */
	public static void putShortVolatile(Object arg0, long arg1, short arg2) {
		UNSAFE.putShortVolatile(arg0, arg1, arg2);
	}




	/**
	 * @param arg0
	 * @return
	 * @deprecated
	 * @see sun.misc.Unsafe#staticFieldBase(java.lang.Class)
	 */
	public static Object staticFieldBase(Class<?> arg0) {
		return UNSAFE.staticFieldBase(arg0);
	}

	/**
	 * @param arg0
	 * @return
	 * @see sun.misc.Unsafe#staticFieldBase(java.lang.reflect.Field)
	 */
	public static Object staticFieldBase(Field arg0) {
		return UNSAFE.staticFieldBase(arg0);
	}

	/**
	 * @param arg0
	 * @return
	 * @see sun.misc.Unsafe#staticFieldOffset(java.lang.reflect.Field)
	 */
	public static long staticFieldOffset(Field arg0) {
		return UNSAFE.staticFieldOffset(arg0);
	}

	/**
	 * @param arg0
	 * @see sun.misc.Unsafe#throwException(java.lang.Throwable)
	 */
	public static void throwException(Throwable arg0) {
		UNSAFE.throwException(arg0);
	}

	/**
	 * @return
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "UnsafeAdapter";
	}

	/**
	 * @param arg0
	 * @return
	 * @see sun.misc.Unsafe#tryMonitorEnter(java.lang.Object)
	 */
	public static boolean tryMonitorEnter(Object arg0) {
		return UNSAFE.tryMonitorEnter(arg0);
	}

	/**
	 * @param arg0
	 * @see sun.misc.Unsafe#unpark(java.lang.Object)
	 */
	public static void unpark(Object arg0) {
		UNSAFE.unpark(arg0);
	}
    
	/** The spin lock value for no lock */
	public static final long NO_LOCK = -1L;
	/** The spin lock value for a shared/read lock */
	public static final long SHARED_LOCK = -2L;
	
	/** The disk spin lock value for no lock */
	public static final PIDThread NO_DISK_LOCK = new PIDThread();	
	
	/**
	 * Allocates an initialized and initially unlocked memory based spin lock
	 * @return the spin lock
	 */
	public static SpinLock allocateSpinLock() {
		long address = allocateAlignedMemory(UnsafeAdapter.LONG_SIZE);
		putLong(address, NO_LOCK);
		return new MemSpinLock(address);
	}
	
	/**
	 * Acquires the lock at the passed address exclusively
	 * @param address The address of the lock
	 * @param barge If true, does not yield between locking attempts. Should only be used by 
	 * a small number of high priority threads, otherwise has no effect.  
	 * @return true if the lock was acquired, false if it was already held by the calling thread
	 */
	public static boolean xlock(final long address, boolean barge) {
		final long tId = Thread.currentThread().getId();
		if(getLong(address)==tId) return false;
		while(!compareAndSwapLong(null, address, NO_LOCK, tId)) {if(!barge) Thread.yield();}
		return true;
	}
	
	/**
	 * Indicates if the spin lock at the specified address is currently held by any thread
	 * @param address The address of the spin lock
	 * @return true if the spin lock at the specified address is currently held by any thread, false otherwise
	 */
	public static boolean xislocked(final long address) {
		return getLong(address)!=NO_LOCK;
	}
	
	/**
	 * Indicates if the spin lock at the specified address is held by the current thread
	 * @param address The address of the spin lock
	 * @return true if the spin lock at the specified address is held by the current thread, false otherwise
	 */
	public static boolean xislockedbyt(final long address) {
		final long tId = Thread.currentThread().getId();
		return getLong(address)==tId;
	}
	
	/**
	 * Acquires the lock at the passed address exclusively with no barging
	 * @param address The address of the lock
	 * @param barge If true, does not yield between locking attempts. Should only be used by 
	 * a small number of high priority threads, otherwise has no effect.  
	 * @return true if the lock was acquired, false if it was already held by the calling thread
	 */
	public static boolean xlock(final long address) {
		return xlock(address, false);
	}
	
	/**
	 * Unlocks the exclusive lock at the passed address if it is held by the calling thread
	 * @param address The address of the lock
	 * @return true if the calling thread held the lock and unlocked it, false if the lock was not held by the calling thread
	 */
	public static boolean xunlock(final long address) {
		final long tId = Thread.currentThread().getId();
		return compareAndSwapLong(null, address, tId, NO_LOCK);
	}
	
	/**
	 * <p>Title: SpinLock</p>
	 * <p>Description: Defines a sping lock impl.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.jmx.util.unsafe.UnsafeAdapterSpinLock</code></p>
	 */
	public interface SpinLock {
		/**
		 * Acquires the lock with the calling thread
		 */
		public void xlock();
		
		/**
		 * Acquires the lock with the calling thread
		 * @param barge  If true, does not yield between locking attempts. Should only be used by 
     	 * a small number of high priority threads, otherwise has no effect.  
		 */
		public void xlock(boolean barge);
		
		
		/**
		 * Releases the lock if it is held by the calling thread
		 */
		public void xunlock();
		
		/**
		 * Indicates if the spin lock is currently held by any thread
		 * @return true if the spin lock is currently held by any thread, false otherwise
		 */
		public boolean isLocked();
		
		/**
		 * Indicates if the spin lock is currently held by the calling thread
		 * @return true if the spin lock is currently held by the calling thread, false otherwise
		 */
		public boolean isLockedByMe();
		
		
	}
	
	/**
	 * <p>Title: MemSpinLock</p>
	 * <p>Description: Unsafe memory based spin lock for use withing JVM only</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.jmx.util.unsafe.UnsafeAdapter.MemSpinLock</code></p>
	 */

	public static class MemSpinLock implements SpinLock, DeAllocateMe {
		/** The lock address */
		protected final long address;

		/**
		 * Creates a new MemSpinLock
		 * @param address The address of the lock
		 */
		private MemSpinLock(long address) {
			this.address = address;
			UnsafeAdapter.registerForDeAlloc(this);
		}

		/**
		 * Returns the lock address
		 * @return the lock address
		 */
		public long address() {
			return address;
		}
		/**
		 * {@inheritDoc}
		 * @see org.helios.jmx.util.unsafe.DeAllocateMe#getAddresses()
		 */
		@Override
		public long[][] getAddresses() {
			return new long[][] {{address}};
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.jmx.util.unsafe.UnsafeAdapter.SpinLock#xlock()
		 */
		@Override
		public void xlock() {
			UnsafeAdapter.xlock(address);
		}
		
		/**
		 * {@inheritDoc}
		 * @see org.helios.jmx.util.unsafe.UnsafeAdapter.SpinLock#xlock(boolean)
		 */
		@Override
		public void xlock(boolean barge) {
			UnsafeAdapter.xlock(address, barge);
		}
		
		/**
		 * {@inheritDoc}
		 * @see org.helios.jmx.util.unsafe.UnsafeAdapter.SpinLock#xunlock()
		 */
		@Override
		public void xunlock() {
			UnsafeAdapter.xunlock(address);
		}
		
		/**
		 * {@inheritDoc}
		 * @see org.helios.jmx.util.unsafe.UnsafeAdapter.SpinLock#isLocked()
		 */
		@Override
		public boolean isLocked() {
			return UnsafeAdapter.xislocked(address);
		}
		
		/**
		 * {@inheritDoc}
		 * @see org.helios.jmx.util.unsafe.UnsafeAdapter.SpinLock#isLockedByMe()
		 */
		@Override
		public boolean isLockedByMe() {
			return UnsafeAdapter.xislockedbyt(address);
		}

	}
	
	/**
	 * <p>Title: MemSpinLock</p>
	 * <p>Description: Disk based spin lock that is sharable with other processes</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.jmx.util.unsafe.UnsafeAdapter.DiskSpinLock</code></p>
	 * FIXME: 
	 */

	public static class DiskSpinLock implements SpinLock, DeAllocateMe {
		/** The lock file name */
		protected final File diskFile;
		/** The mapped lock file address */
		protected final long address;
		/**
		 * Creates a new MemSpinLock
		 * @param address The address of the lock
		 */
		private DiskSpinLock() {
			try {
				diskFile = File.createTempFile("DiskSpinLock", ".spinlock");
				diskFile.deleteOnExit();
				FileChannel fc = new RandomAccessFile(diskFile, "rw").getChannel();
				MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_WRITE, 0, LONG_SIZE * 2);
				address =  ((sun.nio.ch.DirectBuffer) mbb).address();
			} catch (Exception ex) {
				throw new RuntimeException("Failed to allocate disk lock", ex);
			}
			
		}

		/**
		 * Returns the lock address
		 * @return the lock address
		 */
		public long address() {
			return address;
		}
		
		/**
		 * Returns the fully qualified file name of the disk lock
		 * @return the fully qualified file name of the disk lock
		 */
		public String getFileName() {
			return diskFile.getAbsolutePath();
		}
		
		
		/**
		 * {@inheritDoc}
		 * @see org.helios.jmx.util.unsafe.DeAllocateMe#getAddresses()
		 */
		@Override
		public long[][] getAddresses() {
			return new long[][] {{address}};
		}

		/**
		 * Acquires the lock with the calling thread
		 */
		@Override
		public void xlock() {
			xlock(false);
		}
		
/**
		 * {@inheritDoc}
		 * @see org.helios.jmx.util.unsafe.UnsafeAdapter.SpinLock#xlock(boolean)
		 */
		@Override
		public void xlock(boolean barge) {
			final long tId = Thread.currentThread().getId();
			while(!compareAndSwapLong(null, address, NO_LOCK, JVM_PID)) { if(!barge) Thread.yield(); }
			while(!compareAndSwapLong(null, address + LONG_SIZE, NO_LOCK, tId)) { if(!barge) Thread.yield(); }
		}		
		
		/**
		 * Releases the lock if it is held by the calling thread
		 */
		@Override
		public void xunlock() {
			final long tId = Thread.currentThread().getId();
			if(getLong(address)==JVM_PID  &&  getLong(address + LONG_SIZE)==tId) {
				compareAndSwapLong(null, address + LONG_SIZE, tId, NO_LOCK);
				compareAndSwapLong(null, address, JVM_PID, NO_LOCK);
			}
		}

		@Override
		public boolean isLocked() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isLockedByMe() {
			// TODO Auto-generated method stub
			return false;
		}
	}
	
	
	
	/**
	 * <p>Title: PIDThread</p>
	 * <p>Description: </p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.jmx.util.unsafe.UnsafeAdapter.PIDThread</code></p>
	 */
	public static class PIDThread {
		public final long threadId;
		public final long pid;
		
		private PIDThread(long threadId) {
			this.threadId = threadId;
			pid = JVM_PID;
		}
		
		private PIDThread() {
			this.threadId = NO_LOCK;
			this.pid = -1;
		}
		
		public String toString() {
			return String.format("%s@%s", threadId, pid);
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (int) (pid ^ (pid >>> 32));
			result = prime * result + (int) (threadId ^ (threadId >>> 32));
			return result;
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PIDThread other = (PIDThread) obj;
			if (pid != other.pid)
				return false;
			if (threadId != other.threadId)
				return false;
			return true;
		}
		
		
	}
	
	
//	public static boolean slock(final long address) {
//		if(compareAndSwapLong(null, address, NO_LOCK, SHARED_LOCK)) return true;
//		while(!compareAndSwapLong(null, address, NO_LOCK, SHARED_LOCK)) {/* No Op */}
//		return true;
//	}
	
	/**
	 * Executes the passed task after acquiring the exclusive lock the given address.
	 * On completion, the lock is released if it was acquired in this call.
	 * If the lock was already held when on entry, the lock is left in place.
	 * @param address The address of the lock
	 * @param task The task to execute 
	 * @return the return value of the task
	 * @throws Exception thrown if the task itself throws an exception
	 */
	public static <T> T runInLock(final long address, final Callable<T> task) throws Exception {
		final boolean locked = xlock(address);
		try {			
			return task.call();
		} finally {
			if(locked) xunlock(address);
		}
	}
	
	/**
	 * Executes the passed task after acquiring the exclusive lock the given address.
	 * On completion, the lock is released if it was acquired in this call.
	 * If the lock was already held when on entry, the lock is left in place.
	 * @param address The address of the lock
	 * @param task The task to execute 
	 * @return the return value of the task
	 */
	public static double runInLock(final long address, final DoubleCallable task) {
		final boolean locked = xlock(address);
		try {			
			return task.doubleCall();
		} finally {
			if(locked) xunlock(address);
		}
	}
	
	/**
	 * Executes the passed task after acquiring the exclusive lock the given address.
	 * On completion, the lock is released if it was acquired in this call.
	 * If the lock was already held when on entry, the lock is left in place.
	 * @param address The address of the lock
	 * @param task The task to execute 
	 * @return the return value of the task
	 */
	public static long runInLock(final long address, final LongCallable task) {
		final boolean locked = xlock(address);
		try {			
			return task.longCall();
		} finally {
			if(locked) xunlock(address);
		}
	}
	
	/**
	 * Executes the passed task after acquiring the exclusive lock the given address.
	 * On completion, the lock is released if it was acquired in this call.
	 * If the lock was already held when on entry, the lock is left in place.
	 * @param address The address of the lock
	 * @param task The task to execute 
	 * @return the return value of the task
	 */
	public static int runInLock(final long address, final IntCallable task) {
		final boolean locked = xlock(address);
		try {			
			return task.intCall();
		} finally {
			if(locked) xunlock(address);
		}
	}
	
	/**
	 * Executes the passed task after acquiring the exclusive lock the given address.
	 * On completion, the lock is released if it was acquired in this call.
	 * If the lock was already held when on entry, the lock is left in place.
	 * @param address The address of the lock
	 * @param task The task to execute 
	 * @return the return value of the task
	 */
	public static boolean runInLock(final long address, final BooleanCallable task) {
		final boolean locked = xlock(address);
		try {			
			return task.booleanCall();
		} finally {
			if(locked) xunlock(address);
		}
	}
	
	/**
	 * Executes the passed task after acquiring the exclusive lock the given address.
	 * On completion, the lock is released if it was acquired in this call.
	 * If the lock was already held when on entry, the lock is left in place.
	 * @param address The address of the lock
	 * @param task The task to execute 
	 * @return the return value of the task
	 */
	public static byte runInLock(final long address, final ByteCallable task) {
		final boolean locked = xlock(address);
		try {			
			return task.byteCall();
		} finally {
			if(locked) xunlock(address);
		}
	}
	
	
	
	
    
	/**
	 * Executes the passed tasks after acquiring the exclusive lock the given address.
	 * On completion, the lock is released if it was acquired in this call.
	 * If the lock was already held when on entry, the lock is left in place.
	 * @param address The address of the lock
	 * @param tasks The tasks to execute 
	 */
	public static void runInLock(final long address, final Runnable...tasks) {
		final boolean locked = xlock(address);
		try {			
			for(Runnable r: tasks) {
				if(r==null) continue;
				r.run();
			}
		} finally {
			if(locked) xunlock(address);
		}
	}
	
	/** Indicates if this platform is Little Endian */
	public static final boolean littleEndian = ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN);
	/** Indicates if this platform is Big Endian */
	public static final boolean bigEndian = !littleEndian;
	
	 /** A mask to bit switch a byte to an int */
	private static final int UNSIGNED_MASK = 0xFF;
	

    /**
     * Returns true if x1 is less than x2, when both values are treated as unsigned.
     * @author Modified from ASF Cassandra's <a href="https://svn.apache.org/repos/asf/cassandra/trunk/src/java/org/apache/cassandra/utils/FastByteComparisons.java">FastByteComparisons</a>
     */
    static boolean lessThanUnsigned(long x1, long x2) {
      return (x1 + Long.MIN_VALUE) < (x2 + Long.MIN_VALUE);
    }	
    
    /**
     * Returns the value of the given byte as an integer, when treated as
     * unsigned. That is, returns {@code value + 256} if {@code value} is
     * negative; {@code value} itself otherwise.
     * @author From Google Guava's <a href="https://chromium.googlesource.com/external/guava-libraries/+/release14/guava/src/com/google/common/primitives/UnsignedBytes.java">UnsignedBytes</a>.
     */
    public static int toInt(byte value) {
      return value & UNSIGNED_MASK;
    }    

    /**
     * Compares the two specified {@code byte} values, treating them as unsigned
     * values between 0 and 255 inclusive. For example, {@code (byte) -127} is
     * considered greater than {@code (byte) 127} because it is seen as having
     * the value of positive {@code 129}.
     *
     * @param a the first {@code byte} to compare
     * @param b the second {@code byte} to compare
     * @return a negative value if {@code a} is less than {@code b}; a positive
     *     value if {@code a} is greater than {@code b}; or zero if they are equal
     * @author From Google Guava's <a href="https://chromium.googlesource.com/external/guava-libraries/+/release14/guava/src/com/google/common/primitives/UnsignedBytes.java">UnsignedBytes</a>.
     */
    public static int compare(byte a, byte b) {
      return toInt(a) - toInt(b);
    }
    
	
    /**
     * Lexicographically compare two arrays.
     * @param buffer1 left operand
     * @param offset1 Where to start comparing in the left buffer
     * @param length1 How much to compare from the left buffer
	 * @param buffer2 right operand
     * @param offset2 Where to start comparing in the right buffer
     * @param length2 How much to compare from the right buffer
     * @return 0 if equal, < 0 if left is less than right, etc.
     * @author Modified from ASF Cassandra's <a href="https://svn.apache.org/repos/asf/cassandra/trunk/src/java/org/apache/cassandra/utils/FastByteComparisons.java">FastByteComparisons</a>
     */

    
    
    public static int compareTo(byte[] buffer1, int offset1, int length1, byte[] buffer2, int offset2, int length2) {
    	if(buffer1==null || buffer2==null) throw new IllegalArgumentException("Passed a null buffer");
      // Short circuit equal case
      if (buffer1 == buffer2 &&
          offset1 == offset2 &&
          length1 == length2) {
        return 0;
      }
      int minLength = Math.min(length1, length2);
      int minWords = minLength / LONG_SIZE;
      int offset1Adj = offset1 + BYTE_ARRAY_OFFSET;
      int offset2Adj = offset2 + BYTE_ARRAY_OFFSET;

      /*
       * Compare 8 bytes at a time. Benchmarking shows comparing 8 bytes at a
       * time is no slower than comparing 4 bytes at a time even on 32-bit.
       * On the other hand, it is substantially faster on 64-bit.
       */
      for (int i = 0; i < minWords * LONG_SIZE; i += LONG_SIZE) {
        long lw = getLong(buffer1, offset1Adj + (long) i);
        long rw = getLong(buffer2, offset2Adj + (long) i);
        long diff = lw ^ rw;

        if (diff != 0) {
          if (!littleEndian) {
            return lessThanUnsigned(lw, rw) ? -1 : 1;
          }

          // Use binary search
          int n = 0;
          int y;
          int x = (int) diff;
          if (x == 0) {
            x = (int) (diff >>> 32);
            n = 32;
          }

          y = x << 16;
          if (y == 0) {
            n += 16;
          } else {
            x = y;
          }

          y = x << 8;
          if (y == 0) {
            n += 8;
          }
          return (int) (((lw >>> n) & 0xFFL) - ((rw >>> n) & 0xFFL));
        }
      }

      // The epilogue to cover the last (minLength % 8) elements.
      for (int i = minWords * LONG_SIZE; i < minLength; i++) {
        int result = compare(
            buffer1[offset1 + i],
            buffer2[offset2 + i]);
        if (result != 0) {
          return result;
        }
      }
      return length1 - length2;
    }
    
    /**
     * Lexicographically compare two arrays.
     * @param buffer1 left operand
     * @param buffer2 right operand
     * @return 0 if equal, < 0 if left is less than right, etc.
     * @author Modified from ASF Cassandra's <a href="https://svn.apache.org/repos/asf/cassandra/trunk/src/java/org/apache/cassandra/utils/FastByteComparisons.java">FastByteComparisons</a>
     */

    public static int compareTo(byte[] buffer1, byte[] buffer2) {
    	if(buffer1==null || buffer2==null) throw new IllegalArgumentException("Passed a null buffer");
    	return compareTo(buffer1, 0, buffer1.length, buffer2, 0, buffer2.length);
    }
    
//    public static final LongSlidingWindow e = new LongSlidingWindow(150000);
//    public static boolean trace = false;

    /**
     * Lexicographically compare two arrays pointed to by the passed addresses.
     * @param address1 left operand
     * @param size1 the length of the address1 array
     * @param address2 right operand
     * @param size2 the length of the address2 array
     * @return 0 if equal, < 0 if left is less than right, etc.
     * @author Modified from ASF Cassandra's <a href="https://svn.apache.org/repos/asf/cassandra/trunk/src/java/org/apache/cassandra/utils/FastByteComparisons.java">FastByteComparisons</a>
     */

    public static boolean compareTo(long address1, int size1, long address2, int size2) {
//    	long start = System.nanoTime();    	
    	return byteArraysEqual(getByteArray(address1, size1), getByteArray(address2, size2));
//    	if(trace) e.insert(System.nanoTime()-start);
//    	return r;
    }
    
    
    public static boolean charAraysEqual(char[] c1, char[] c2) {
    	return compareTo(getAddressOf(c1) + CHAR_ARRAY_OFFSET, c1.length, getAddressOf(c2) + CHAR_ARRAY_OFFSET, c2.length);
    }
    
    public static boolean byteArraysEqual(byte[] b1,byte[] b2)  {   
      if(b1 == b2) {
        return true;
      }
      if(b1.length != b2.length) {
        return false;
      }
      int baseOffset = arrayBaseOffset(byte[].class);
      int numLongs = (int)Math.ceil(b1.length / 8.0);
      for(int i = 0;i < numLongs; ++i)  {
        long currentOffset = baseOffset + (i * 8);
        long l1 = getLong(b1, currentOffset);
        long l2 = getLong(b2, currentOffset);
        if(0L != (l1 ^ l2)) {
          return false;
        }
      }
      return true;    
    }




    
//    public static char[] copyStringChars(String string) {
//    	if(string==null) throw new IllegalArgumentException("The passed string was null");
//    	int count = getInt(string, STRING_COUNT_OFFSET);
//    	int offset = getInt(string, STRING_ARR_OFFSET);
//    	int arrSize = count - offset;
//    	//       to the array ----\/    to the array data ---\/  plus offset
//    	long addressOffset = STRING_CHARS_OFFSET + CHAR_ARRAY_OFFSET + offset + 8; 
//    	LOG.info("ChOffset: {}, AddrOffset: {}, ArrSize: {}, Count: {}, Offset: {}", CHAR_ARRAY_OFFSET, addressOffset, arrSize, count, offset);
//    	
//    	char[] arr = new char[arrSize];
//    	copyMemory(string, addressOffset, arr, CHAR_ARRAY_OFFSET, arrSize * CHAR_SIZE);
//    	return arr;
////        STRING_CHARS_OFFSET = UNSAFE.objectFieldOffset(String.class.getDeclaredField("value"));
////        STRING_ARR_OFFSET = UNSAFE.objectFieldOffset(String.class.getDeclaredField("offset"));
////        STRING_COUNT_OFFSET = UNSAFE.objectFieldOffset(String.class.getDeclaredField("count"));
//    }
//    
//    public static void main(String[] args) {
//    	String s = "Goodbye Jupiter";
//    	char[] arr = copyStringChars(s);
//    	LOG.info("Char Arr: {}\n\tLength: {}", Arrays.toString(arr), arr.length);
//    }
	
}
