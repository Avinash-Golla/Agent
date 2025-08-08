package com.yourorg.agent;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class RepoCloner {

	public static void cloneOrUpdateRepo(String repoUrl, String localPath) {
		File repoDir = new File(localPath);
		File gitDir = new File(repoDir, ".git");

		try {
			if (repoDir.exists()) {
				if (gitDir.exists()) {
					System.out.println("🔄 Repository already exists. Pulling latest changes...");
					Git git = Git.open(repoDir);
					git.pull().call();
					System.out.println("✅ Repository updated successfully.");
					return;
				} else {
					System.out.println("⚠️ Folder exists but is not a Git repo. Deleting it...");
					deleteDirectory(repoDir);
				}
			}

			System.out.println(" Cloning repository for the first time...");
			Git.cloneRepository()
					.setURI(repoUrl)
					.setDirectory(repoDir)
					.call();
			System.out.println(" Repository cloned successfully.");
		} catch (GitAPIException | IOException e) {
			throw new RuntimeException("Failed to clone or pull repository", e);
		}
	}

	private static void deleteDirectory(File dir) throws IOException {
		Files.walk(dir.toPath())
				.sorted((a, b) -> b.compareTo(a)) // reverse order to delete children first
				.forEach(path -> {
					try {
						Files.delete(path);
					} catch (IOException e) {
						throw new RuntimeException(" Failed to delete " + path, e);
					}
				});
	}
}
