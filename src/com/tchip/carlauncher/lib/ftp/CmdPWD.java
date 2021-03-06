package com.tchip.carlauncher.lib.ftp;

import java.io.File;
import java.io.IOException;

import android.util.Log;

public class CmdPWD extends FtpCmd implements Runnable {
	// public static final String message = "TEMPLATE!!";

	public CmdPWD(SessionThread sessionThread, String input) {
		super(sessionThread, CmdPWD.class.toString());
	}

	public void run() {
		myLog.l(Log.DEBUG, "PWD executing");

		// We assume that the chroot restriction has been applied, and that
		// therefore the current directory is located somewhere within the
		// chroot directory. Therefore, we can just slice of the chroot
		// part of the current directory path in order to get the
		// user-visible path (inside the chroot directory).
		try {
			File workingDir = sessionThread.getWorkingDir();
			String currentDir = workingDir != null ? workingDir
					.getCanonicalPath() : Globals.getChrootDir()
					.getCanonicalPath();
			currentDir = currentDir.substring(Globals.getChrootDir()
					.getCanonicalPath().length());
			// The root directory requires special handling to restore its
			// leading slash
			if (currentDir.length() == 0) {
				currentDir = "/";
			}
			sessionThread.writeString("257 \"" + currentDir + "\"\r\n");
		} catch (IOException e) {
			// This shouldn't happen unless our input validation has failed
			myLog.l(Log.ERROR, "PWD canonicalize");
			sessionThread.closeSocket(); // should cause thread termination
		}
		myLog.l(Log.DEBUG, "PWD complete");
	}

}
