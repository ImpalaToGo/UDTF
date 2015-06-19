// Copyright 2015 Impala2go team (Impala2go.info)
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.impala2go.seq;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * @author elenav 
 * Primitive utility to emulate Hive explode() - see  
 * https://cwiki.apache.org/confluence/display/Hive/LanguageManual+UDF#LanguageManualUDF-explode
 * on Hadoop Sequence file - see 
 * http://wiki.apache.org/hadoop/SequenceFile
 * 
 * Utility reports its execution status using System.exit(error_code).
 */
public class SequenceFileExploder {

	/* root directory to hold the temporary in */
	public static final String ROOT_TMP_PATH = "/tmp/";
	
	/* Hadoop local file system protocol */
	public static final String HADOOP_LOCAL_FS = "file:///";
	
	enum TempMgmt {
	    SETUP,
	    TEARDOWN
	}
	
	enum Status{
		OK, 
		ARGS_ERR,
		TEMP_FILE_MGMT_ERR,
		TEMP_FILE_WRITTING_ERR,
		SEQUENCE_READ_ERR
	}
	
	/**
	 * Check program arguments. Sequence File Exploder looks for 
	 * the only argument, original file name, in order to create unique temporary file
	 * as a prerequisite for Hadoop Sequence Reader invocation.
	 * 
	 * @param args - program arguments
	 */
	static void checkArguments(String[] args){
	}
	
	/**
	 * Creates the file along will all non-existing hierarchy of folders
	 * 
	 * @param path - fqp to create
	 * 
	 * @throws IOException 
	 */
	public static void createFileAndHierarchy(String path) throws IOException{
        // create the temporary file to collect the standard input:
		File temp_file = new File(path);
		// create the directories hierarchy if any requested:
		temp_file.getParentFile().mkdirs(); 
		// create the temporary file:
		temp_file.createNewFile();
	}
	
	/**
	 * Recursively deletes the whole parent hierarchy of the given @a path,
	 * up to "root" directory (should be specified)
	 * If the hierarchy node is not the empty one, it is preserved, and the deletion is interrupted.
	 * 
	 * @param path - path to delete recursively
	 * 
	 * @throws IOException
	 */
	public static void removeFileRecursivelyPreserveNonEmptyParent(Path path)
	        throws IOException {
		
		// all required deletions are completed:
	    if(path == null || path.endsWith(ROOT_TMP_PATH)) 
	    	return;

	    if (Files.isRegularFile(path)) {
	        Files.deleteIfExists(path);
	    } else if(Files.isDirectory(path)) {
	        try {
	            Files.delete(path);
	        } catch(DirectoryNotEmptyException e) {
	            return;
	        }
	    }
	    removeFileRecursivelyPreserveNonEmptyParent(path.getParent());
	}
	
	/**
	 * Manages temporaries on the filesystem within the program lifecycle
	 * 
	 * @param path     - path to be managed
	 * @param mgmtType - mgmt operation type (create/delete)
	 */
	static void manageTemporary(String path, TempMgmt mgmtType){
		try {
			switch (mgmtType) {
			case SETUP:
				createFileAndHierarchy(path);
				break;
			case TEARDOWN:
				removeFileRecursivelyPreserveNonEmptyParent(Paths.get(path));
				break;
			}
		} catch (IOException io) {
			System.exit(Status.TEMP_FILE_MGMT_ERR.ordinal());
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// validate arguments:
		checkArguments(args);

		// The root directory to hold the temporary may be optionally specified.
		// compose path for temporary file:
		String root = ROOT_TMP_PATH;
		String uniqueID = UUID.randomUUID().toString();
		String path = root + uniqueID;
		
		// run prerequisites (temporary file creation):
		manageTemporary(path, TempMgmt.SETUP);

		OutputStream outputStream = null;
		ByteArrayOutputStream baos = null;
		
		// Read the standard input until eof and write it into temporary:
		try {
			try {
				outputStream = new FileOutputStream(path);
				baos = new ByteArrayOutputStream();
				byte[] buffer = new byte[32 * 1024];

				int bytesRead;
				// Read the standard input until eof:
				while ((bytesRead = System.in.read(buffer)) > 0) {
					baos.write(buffer, 0, bytesRead);
				}
				// write into temporary
				baos.writeTo(outputStream);
			} finally {
				baos.close();
				outputStream.close();
			}
		} catch (IOException io) {
			manageTemporary(path, TempMgmt.TEARDOWN);
			System.exit(Status.TEMP_FILE_WRITTING_ERR.ordinal());
		}
			    
		// pass the standard output handle to the Sequence File Reader:
		OutputStream output = System.out;       
		try {
			try{
				HadoopClient.readSequenceFile(path, HADOOP_LOCAL_FS, output);
				}
			finally{
				output.close();
				}
			} catch (Exception e) {
				System.exit(Status.SEQUENCE_READ_ERR.ordinal());
				} 
		finally{
			// run prerequisites (temporary file creation):
			manageTemporary(path, TempMgmt.TEARDOWN);
		}
		// System.exit(0);
		}
}
