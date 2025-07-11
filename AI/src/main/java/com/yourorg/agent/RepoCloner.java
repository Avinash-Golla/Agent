package com.yourorg.agent;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;

public class RepoCloner {

	public static void cloneOrUpdateRepo(String repoUrl, String localPath) {
		// 👇 This becomes ./repo/automation-tests
		File repoDir = new File(localPath, "automation-tests");

		try {
			if (repoDir.exists() && new File(repoDir, ".git").exists()) {
				System.out.println("Repository already exists. Pulling latest changes...");
				Git git = Git.open(repoDir);
				git.pull().call();
				System.out.println("Repository updated successfully.");
			} else {
				System.out.println("Cloning repository for the first time...");
				Git.cloneRepository()
						.setURI(repoUrl)
						.setDirectory(repoDir)
						.call();
				System.out.println("Repository cloned successfully.");
			}
		} catch (GitAPIException | IOException e) {
			throw new RuntimeException("Failed to clone or pull repository", e);
		}
	}
}
