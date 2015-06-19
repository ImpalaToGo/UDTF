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

import java.io.IOException;
import java.io.OutputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;

/**
 * Hadoop Client, bridges the Hadoop functionality
 * @author elenav
 */
public class HadoopClient {
	/** Hadoop configuration */
	private static Configuration conf = new Configuration();
	
	/** default fs to run Hadoop for */
	private static final String DEFAULT_FS = "fs.defaultFS";
	

	/**
	 * Read a Hadoop sequence file and stream values out (into @a out)
	 * 
	 * @param seqPath
	 *            Full path to of the sequence file to read (excluding fs
	 *            protocol)
	 * @param fs
	 *            Hadoop file system address
	 * 
	 * @throws IOException
	 * @throws IllegalAccessException related to reflection 
	 * @throws InstantiationException related to reflection
	 */
	@SuppressWarnings("rawtypes")
	public static void readSequenceFile(String seqPath, String fs,
			OutputStream out) throws IOException, InstantiationException, IllegalAccessException {
		
		conf.set(DEFAULT_FS, fs);
		Path path = new Path(seqPath);
		
		// create the reader:
		SequenceFile.Reader reader = new SequenceFile.Reader(conf,
				SequenceFile.Reader.file(path));
		
		WritableComparable key = null;
		Writable value = null;

		// load key and value classes:
		key = (WritableComparable) reader.getKeyClass().newInstance();
		value = (Writable) reader.getValueClass().newInstance();
		
		// read and stream out, values only:
		while (reader.next(key, value)) {
			//System.out.println(value);
			out.write(value.toString().getBytes());
			out.flush();
		}
		IOUtils.closeStream(reader);
	}

}