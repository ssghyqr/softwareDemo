import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.RemoteRefUpdate.Status;
import org.eclipse.jgit.transport.TrackingRefUpdate;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class JGitCommandsLearningTest {
    private static final String MASTER = "refs/heads/master";
    private static final String ORIGIN_MASTER = "refs/remotes/origin/master";

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    private Git remote;
    private Git local;

    private Git initRepository() throws GitAPIException, IOException {
        return Git.init().setDirectory(tempFolder.newFolder("remote")).call();
    }

    private Git cloneRepository() throws GitAPIException, IOException {
        String remoteUri = remote.getRepository().getDirectory().getCanonicalPath();
        File localDir = tempFolder.newFolder("local");
        return Git.cloneRepository().setURI(remoteUri).setDirectory(localDir).call();
    }

    private File createFile(String name) throws IOException {
        File file = new File(local.getRepository().getWorkTree(), name);
//        System.out.println(local.getRepository().getWorkTree());
//        System.out.println(name);
        file.createNewFile();
        return file;
    }

    @Before
    public void setUp() throws GitAPIException, IOException {
        remote = initRepository();
        local = cloneRepository();
    }

    @After
    public void tearDown() {
        remote.close();
        local.close();
    }

    /**
     * 本地创建一个临时文件
     * @throws IOException
     * @throws GitAPIException
     */
    @Test
    public void testAdd() throws IOException, GitAPIException {
        File file = createFile("readme.txt");
        DirCache index = local.add().addFilepattern(file.getName()).call();
        assertEquals(1, index.getEntryCount());
        assertEquals(file.getName(), index.getEntry(0).getPathString());
    }

    /**
     * 提交之前
     * @throws IOException
     * @throws GitAPIException
     */
    @Test
    public void testStatusBeforeAdd() throws IOException, GitAPIException {
        File file = createFile("readme.txt");
        org.eclipse.jgit.api.Status status = local.status().call();
//        这是一个单例模式
        System.out.println(singleton(file.getName()) + " " + status.getUntracked());
        assertEquals(singleton(file.getName()), status.getUntracked());
    }

    /**
     * 提交之后
     * @throws IOException
     * @throws GitAPIException
     */
    @Test
    public void testStatusAfterAdd() throws IOException, GitAPIException {
        File file = createFile("readme.txt");
        local.add().addFilepattern(file.getName()).call();
        org.eclipse.jgit.api.Status status = local.status().call();

//        提交之后,文件状态会变成未跟踪
        assertEquals(singleton(file.getName()), status.getAdded());
        assertTrue(status.getUntracked().isEmpty());
    }

    /**
     * 测试有文件路径的文件状态
     * @throws Exception
     */
    @Test
    public void testStatusWithPath() throws Exception {
        File file = createFile("readme.txt");
        createFile("unrelated.txt");

        org.eclipse.jgit.api.Status status = local.status().addPath("readme.txt").call();

        assertEquals(singleton(file.getName()), status.getUntracked());
    }

    /**
     * 测试未存在文件路径的文件状态
     * @throws Exception
     */
    @Test
    public void testStatusWithNonExistingPath() throws Exception {
        createFile("readme.txt");
        org.eclipse.jgit.api.Status status = local.status().addPath("does-not-exist").call();
        assertTrue(status.isClean());
    }

    /**
     * 测试添加不存在的文件
     * @throws GitAPIException
     */
    @Test
    public void testAddNonExistingFile() throws GitAPIException {
        DirCache index = local.add().addFilepattern("foo").call();
        assertEquals(0, index.getEntryCount());
    }

    /**
     * 添加一个文件图案
     * @throws IOException
     * @throws GitAPIException
     */
    @Test
    public void testAddPattern() throws IOException, GitAPIException {
        File file = createFile("readme.txt");
        DirCache index = local.add().addFilepattern(".").call();
        assertEquals(1, index.getEntryCount());
        assertEquals(file.getName(), index.getEntry(0).getPathString());
    }

    /**
     * 提交
     * @throws IOException
     * @throws GitAPIException
     */
    @Test
    public void testCommit() throws IOException, GitAPIException {
        File file = createFile("readme.txt");
        local.add().addFilepattern(file.getName()).call();
        String message = "create file";
        RevCommit commit = local.commit().setMessage(message).call();
        assertEquals(message, commit.getFullMessage());
    }

    /**
     * 提交但不带上信息
     * @throws Exception
     */
    @Test(expected = NoMessageException.class)
    public void CommitWithoutMessage() throws Exception {
        local.commit().call();
    }

    /**
     * 提交但是空信息
     * @throws GitAPIException
     */
    @Test
    public void testCommitWithEmptyMessage() throws GitAPIException {
        RevCommit commit = local.commit().setMessage("").call();

        assertEquals("", commit.getFullMessage());
    }

    /**
     * 移除pattern
     * @throws IOException
     * @throws GitAPIException
     */
    @Test
    public void testRm() throws IOException, GitAPIException {
        File file = createFile("readme.txt");
        local.add().addFilepattern(file.getName()).call();
        DirCache index = local.rm().addFilepattern(file.getName()).call();
        assertEquals(0, index.getEntryCount());
        assertFalse(file.exists());
    }

    /**
     * 删除文件上的pattern
     * @throws IOException
     * @throws GitAPIException
     */
    @Test
    public void testRmCached() throws IOException, GitAPIException {
        File file = createFile("readme.txt");
        local.add().addFilepattern(file.getName()).call();
        DirCache index = local.rm().setCached(true).addFilepattern(file.getName()).call();
        assertEquals(0, index.getEntryCount());
        assertTrue(file.exists());
    }

    /**
     * 不存在的文件去除pattern
     * @throws GitAPIException
     */
    @Test
    public void testRmNonExistingFile() throws GitAPIException {
        DirCache index = local.rm().addFilepattern("foo").call();
        assertEquals(0, index.getEntryCount());
    }

    /**
     * 获取日志
     * @throws GitAPIException
     */
    @Test
    public void testLog() throws GitAPIException {
        RevCommit commit = local.commit().setMessage("empty commit").call();
        Iterable<RevCommit> iterable = local.log().call();
        List<RevCommit> commits = stream(iterable.spliterator(), false).collect(toList());
        assertEquals(1, commits.size());
        assertEquals(commit, commits.get(0));
    }

    /**
     * 获取所有提交的记录
     * @throws IOException
     * @throws GitAPIException
     */
    @Test
    public void testRevWalk() throws IOException, GitAPIException {
        RevCommit initialCommit = local.commit().setMessage("init commit").call();
        Ref branch = local.branchCreate().setName("side").call();
        local.checkout().setName(branch.getName()).call();
        RevCommit branchCommit = local.commit().setMessage("commit on side branch").call();
        local.checkout().setName(MASTER).call();

        List<RevCommit> commits;
        try (RevWalk revWalk = new RevWalk(local.getRepository())) {
            ObjectId commitId = local.getRepository().resolve(branch.getName());
            revWalk.markStart(revWalk.parseCommit(commitId));
            commits = stream(revWalk.spliterator(), false).collect(toList());
        }

        assertEquals(2, commits.size());
        assertEquals(branchCommit, commits.get(0));
        assertEquals(initialCommit, commits.get(1));
    }

    /**
     * 推送代码
     * @throws IOException
     * @throws GitAPIException
     */
    @Test
    public void testPush() throws IOException, GitAPIException {
        RevCommit commit = local.commit().setMessage("local commit").call();

        Iterable<PushResult> iterable = local.push().call();

        RemoteRefUpdate remoteUpdate = iterable.iterator().next().getRemoteUpdate(MASTER);
        assertEquals(Status.OK, remoteUpdate.getStatus());
        assertEquals(commit, remoteUpdate.getNewObjectId());
        assertEquals(commit.getId(), remote.getRepository().resolve(MASTER));
    }

    @Test
    public void testFetch() throws IOException, GitAPIException {
        RevCommit commit = remote.commit().setMessage("remote commit").call();
        FetchResult fetchResult = local.fetch().call();
        TrackingRefUpdate refUpdate = fetchResult.getTrackingRefUpdate(ORIGIN_MASTER);
        assertEquals(RefUpdate.Result.NEW, refUpdate.getResult());
        assertEquals(commit.getId(), local.getRepository().resolve(ORIGIN_MASTER));
    }
}
