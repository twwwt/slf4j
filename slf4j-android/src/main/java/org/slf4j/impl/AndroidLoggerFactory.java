/*
 * Created 21.10.2009
 *
 * Copyright (c) 2009-2012 SLF4J.ORG
 *
 * All rights reserved.
 *
 * Permission is hereby granted, free  of charge, to any person obtaining
 * a  copy  of this  software  and  associated  documentation files  (the
 * "Software"), to  deal in  the Software without  restriction, including
 * without limitation  the rights to  use, copy, modify,  merge, publish,
 * distribute,  sublicense, and/or sell  copies of  the Software,  and to
 * permit persons to whom the Software  is furnished to do so, subject to
 * the following conditions:
 *
 * The  above  copyright  notice  and  this permission  notice  shall  be
 * included in all copies or substantial portions of the Software.
 *
 * THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
 * EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
 * MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.slf4j.impl;

import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.ILoggerFactory;

import android.util.Log;

/**
 * An implementation of {@link ILoggerFactory} which always returns
 * {@link AndroidLogger} instances.
 *
 * @author Thorsten M&ouml;ler
 * @version $Rev:$; $Author:$; $Date:$
 */
public class AndroidLoggerFactory implements ILoggerFactory
{
	private final ConcurrentMap<String, AndroidLogger> loggerMap;

	static final int TAG_MAX_LENGTH = 23; // tag names cannot be longer on Android platform
                                         // see also android/system/core/include/cutils/property.h
                                         // and android/frameworks/base/core/jni/android_util_Log.cpp

	public AndroidLoggerFactory()
	{
		loggerMap = new ConcurrentHashMap<String, AndroidLogger>();
	}

	/* @see org.slf4j.ILoggerFactory#getLogger(java.lang.String) */
	public AndroidLogger getLogger(final String name)
	{
		final String tag = forceValidName(name); // fix for bug #173

		AndroidLogger logger = loggerMap.get(tag);
		if (logger != null) return logger;

		logger = new AndroidLogger(tag);
		AndroidLogger loggerPutBefore = loggerMap.putIfAbsent(tag, logger);
		if (null == loggerPutBefore)
		{
			if (!tag.equals(name) && Log.isLoggable(AndroidLoggerFactory.class.getSimpleName(), Log.WARN))
			{
				Log.i(AndroidLoggerFactory.class.getSimpleName(), new StringBuilder("SLF4J Logger name '")
				.append(name)
				.append("' exceeds maximum length of ")
				.append(TAG_MAX_LENGTH)
				.append(" characters; using '")
				.append(tag)
				.append("' as the Android Log tag instead.")
				.toString());
			}
			return logger;
		}
		return loggerPutBefore;
	}

	/**
	 * Trim name in case it exceeds maximum length of {@value #TAG_MAX_LENGTH} characters.
	 */
	private final String forceValidName(String name)
	{
		if (name != null && name.length() > TAG_MAX_LENGTH)
		{
			final StringTokenizer st = new StringTokenizer(name, ".");
			if (st.hasMoreTokens()) // note that empty tokens are skipped, i.e., "aa..bb" has tokens "aa", "bb"
			{
				final StringBuilder sb = new StringBuilder();
				String token;
				do
				{
					token = st.nextToken();
					if (token.length() == 1) // token of one character appended as is
					{
						sb.append(token);
					}
					else if (st.hasMoreTokens()) // truncate all but the last token
					{
						sb.append(token.charAt(0));
						sb.append('*');
					}
					else // last token (usually class name) appended as is
					{
						sb.append(token);
					}
					if (st.hasMoreTokens())
					{
						sb.append('.');
					}
				} while (st.hasMoreTokens());

				name = sb.toString();
			}

			// Either we had no useful dot location at all or name still too long.
			// Take leading part and append '*' to indicate that it was truncated
			if (name.length() > TAG_MAX_LENGTH)
			{
				name = name.substring(0, TAG_MAX_LENGTH - 1) + '*';
			}
		}
		return name;
	}
}
