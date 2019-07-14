package io.github.jeog.tdameritradeapi;

import java.io.File;
import java.util.Map;

import com.sun.jna.ptr.PointerByReference;

import com.sun.jna.Native;
import com.sun.jna.Platform;

public final class TDAmeritradeAPI {
    
    public static final String LIBRARY_NAME = "TDAmeritradeAPI";
        
    @SuppressWarnings("serial")
    public static class LibraryNotLoaded extends RuntimeException{
        LibraryNotLoaded(){
            super(LIBRARY_NAME + " not loaded.");
        }
        LibraryNotLoaded(String msg){
            super(LIBRARY_NAME + " not loaded: " + msg);
        }
    }    

    @SuppressWarnings("serial")
    public static class CLibException extends Exception{    
        /*
         * IMPORTANT - be sure not to throw CLibException in here
         */        
        public int errorCode;
        
        public CLibException(int errorCode) {
            super( buildMessage(errorCode) );
            this.errorCode = errorCode;            
        }
        
        static private String
        buildMessage(int errorCode) {
            StringBuilder sb = new StringBuilder();
            String name = Error.getErrorName(errorCode);        
            sb.append(name == null ? "INVALID ERROR CODE" : name).append(": ");
            Map<String, String> info = Error.lastErrorInfo();
            if( info == null )
                sb.append("failed to retrieve error info from library");
            else {
                String c = info.get("code");
                if( !c.equals(String.valueOf(errorCode)) )
                    sb.append("bad error code state[").append(errorCode).append("," + c + "]");                    
                else {
                    sb.append("[");
                    for( String k : info.keySet() ) 
                        sb.append(k).append(": ").append( info.get(k) ).append(", ");
                    int l = sb.length();
                    sb.delete(l-2, l).append("]");                   
                }
            }
            return sb.toString();
        }
    }
    
    private static CLib library = null;
    
    /* NOTE - shouldn't be used directly by client code */
    public static final CLib
    getCLib() {
        if( library == null )
            throw new LibraryNotLoaded();
        return library;        
    }
    
    public static boolean
    init(String path) {
        if( library != null )
            return true;
        
        try {
            File file = new File(path);
            String name = file.getName();
            int pPos = name.lastIndexOf(".");
            if( pPos > 0 )
                name = name.substring(0, pPos);
            if( !Platform.isWindows() && name.startsWith("lib") )
                name = name.substring(3);
            
            String dir = file.getParentFile().getPath();
            
            System.out.println(LIBRARY_NAME+ " :: init :: "+ "directory: "+ dir + " name: " + name);
            System.setProperty("jna.library.path",  dir);
            
            library = Native.load(name, CLib.class);            
        }catch( Throwable t ) {
            System.err.println(LIBRARY_NAME+ " :: init :: failed: " + t.getMessage());
            t.printStackTrace(System.err);
            throw new LibraryNotLoaded(t.getMessage());
        }
        
        try {
            Error.lastErrorCode();
        }catch( CLibException exc ) {
            System.err.println(LIBRARY_NAME+ " :: init :: failed test call: " + exc.getMessage());
            return false;
        }        
        
        return true;
    }

    
    public static String
    buildOptionSymbol(String underlying, int month, int day, int year, boolean is_call, double strike) 
            throws CLibException {        
        CLib.size_t[] n = new CLib.size_t[1];
        PointerByReference p = new PointerByReference();
                
        int err = getCLib().BuildOptionSymbol_ABI(underlying, month, day, year, is_call ? 1 : 0, strike, p, n, 0);
        if( err != 0 )
            throw new CLibException(err);
        
        return CLib.Helpers.stringFromBuffer(p.getValue(), n[0].intValue());   
    }
    
    public static void
    checkOptionSymbol(String symbol) throws CLibException {
        int err = getCLib().CheckOptionSymbol_ABI(symbol, 0);
        if( err != 0 )
            throw new CLibException(err);        
    }

    public static void
    test() {
        //
    }

    
}
