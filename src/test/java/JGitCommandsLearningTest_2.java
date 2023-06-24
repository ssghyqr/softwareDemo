import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.*;


import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.FS;
import org.junit.Test;

public class JGitCommandsLearningTest_2 {
    // 本地仓库位置
    public final String localPath = "E:\\Software3";
    // 测试克隆仓库地址
    public final String localClonePath = "E:\\Software2";
    // 私钥地址
    public final String keyPath = "C:\\Users\\28458\\.ssh\\id_rsa";
    // passphrase,也是ssh必须的
    public final String passphrase = "667622yuan";
    // ssh地址
    public final String sshUri = "git@github.com:ssghyqr/software.git";
    // 会话工厂,ssh连接必须的工厂
    public final SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
        @Override
        protected void configure(OpenSshConfig.Host host, Session session) {
            session.setConfig("StrictHostKeyChecking", "no");
        }

        @Override
        protected JSch createDefaultJSch(FS fs) throws JSchException {
            JSch sch = super.createDefaultJSch(fs);
            //keyPath 私钥文件 path
//                这一步的passphrase是必须的
            sch.addIdentity(keyPath, passphrase);
            return sch;
        }
    };

    /**
     * 目录（文件夹）删除的方法
     *
     * @param path
     */
    public void deleteDir(String path) {
        // 为传进来的路径参数创建一个文件对象
        File file = new File(path);
        // 如果目标路径是一个文件，那么直接调用delete方法删除即可
        // file.delete();
        // 如果是一个目录，那么必须把该目录下的所有文件和子目录全部删除，才能删除该目标目录，这里要用到递归函数
        // 创建一个files数组，用来存放目标目录下所有的文件和目录的file对象
        // 将目标目录下所有的file对象存入files数组中
        File[] files = Objects.requireNonNull(file.listFiles());
        // 循环遍历files数组
        for (File temp : files) {
            // 判断该temp对象是否为文件对象
            if (temp.isFile()) {
                temp.delete();
            }
            // 判断该temp对象是否为目录对象
            if (temp.isDirectory()) {
                // 将该temp目录的路径给delete方法（自己），达到递归的目的
                deleteDir(temp.getAbsolutePath());
                // 确保该temp目录下已被清空后，删除该temp目录
                temp.delete();
            }
        }
    }

    /**
     * 转换类
     *
     * @param repository
     * @param objectId
     * @return
     * @throws IOException
     */
    public static AbstractTreeIterator prepareTreeParser(Repository repository, String objectId) throws IOException {
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(ObjectId.fromString(objectId));
            RevTree tree = walk.parseTree(commit.getTree().getId());
            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            try (ObjectReader reader = repository.newObjectReader()) {
                treeParser.reset(reader, tree.getId());
            }
            walk.dispose();
            return treeParser;
        }
    }


    /**
     * 获取git仓库单例对象
     *
     * @param dir
     * @return
     */
    public static Git openRpo(String dir) {
        Git git = null;
        try {
//            配置的一个仓库
            Repository repository = new FileRepositoryBuilder()
                    .setGitDir(Paths.get(dir, ".git").toFile())
                    .build();
//            仓库得到一个git对象
            git = new Git(repository);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return git;
    }

    /**
     * 打开本地仓库
     */
    @Test
    public void gitBashHere() {
        Git git = openRpo(localPath);
        System.out.println(git);
    }

    /**
     * 初始化本地仓库
     *
     * @throws GitAPIException
     */
    @Test
    public void gitInit() throws GitAPIException {
        Git git = Git.init().setDirectory(new File(localPath)).call();
        System.out.println(git);
    }

    /**
     * 添加到暂存区
     *
     * @throws GitAPIException
     */
    @Test
    public void gitAdd() throws GitAPIException {
        Git git = openRpo(localPath);
        git.add().addFilepattern("add.txt").call();
        git.add().addFilepattern("delete.txt").call();
        git.add().addFilepattern("modify.txt").call();
        git.rm().addFilepattern("delete.txt").call();
    }

    /**
     * 暂存区提交
     *
     * @throws GitAPIException
     */
    @Test
    public void gitCommit() throws GitAPIException {
        Git git = openRpo(localPath);

        git.commit().setMessage("first commit").call();
    }

    /**
     * 暂存区文件状态
     *
     * @throws GitAPIException
     */
    @Test
    public void gitStatus() throws GitAPIException {
        Map<String, String> map = new HashMap<String, String>();
        Git git = openRpo(localPath);

        Status status = git.status().call();
        map.put("Added", status.getAdded().toString());
        map.put("Changed", status.getChanged().toString());
        map.put("Conflicting", status.getConflicting().toString());
        map.put("ConflictingStageState", status.getConflictingStageState().toString());
        map.put("IgnoredNotInIndex", status.getIgnoredNotInIndex().toString());
        map.put("Missing", status.getMissing().toString());
        map.put("Modified", status.getModified().toString());
        map.put("Removed", status.getRemoved().toString());
        map.put("UntrackedFiles", status.getUntracked().toString());
        map.put("UntrackedFolders", status.getUntrackedFolders().toString());
        System.out.println(map);
    }

    /**
     * 所有分支
     *
     * @throws GitAPIException
     */
    @Test
    public void gitBranchList() throws GitAPIException {
        Git git = openRpo(localPath);

        //得到所有分支信息
        List<Ref> callList = git.branchList().call();
        for (Ref ref : callList)
            System.out.println(ref.getName());
    }

    /**
     * 创建分支
     *
     * @throws GitAPIException
     */
    @Test
    public void gitBranchAddDev() throws GitAPIException {
        Git git = openRpo(localPath);

        List<Ref> callList = git.branchList().call();
        if (callList.size() <= 0) {
//            没有提交过,没有master分支先提交一次
            git.commit().setMessage("first commit").call();
        } else {
            for (Ref ref : callList) {
                if (ref.getName().split("/")[2].equals("dev")) {
                    System.out.println("dev exist");
                } else {
                    if (callList.size() <= 1) {
                        git.branchCreate().setName("dev").call();
                    }
                }
            }
        }
//
    }

    /**
     * 删除分支
     *
     * @throws GitAPIException
     */
    @Test
    public void gitBranchDelDev() throws GitAPIException {
        Git git = openRpo(localPath);

        //设置删除分支的名字
        git.branchDelete().setBranchNames("dev").call();
    }

    /**
     * 跳转分支
     *
     * @throws GitAPIException
     */
    @Test
    public void gitBranchCheckout() throws GitAPIException {
        Git git = openRpo(localPath);

        //设置分支名
        git.checkout().setName("dev").call();
    }

    /**
     * 合并dev和master
     *
     * @throws GitAPIException
     */
    @Test
    public void gitMerge() throws GitAPIException {
        Git git = openRpo(localPath);

        Ref refdev = git.checkout().setName("dev").call(); //切换分支获取分支信息存入Ref对象里
        git.checkout().setName("master").call();   //切换回master分支
        MergeResult mergeResult = git.merge().include(refdev)  // 合并目标分支
                .setCommit(true)           //同时提交
                .setFastForward(MergeCommand.FastForwardMode.NO_FF)// 分支合并策略NO_FF代表普通合并 //  FF代表快速合并
                .setMessage("master Merge dev")     //设置提交信息
                .call();
    }

    /**
     * 获取remote列表
     *
     * @throws GitAPIException
     */
    @Test
    public void gitRemoteList() throws GitAPIException {
        Git git = openRpo(localPath);

        Map<String, String> urlMap = new HashMap<>();
        List<RemoteConfig> remoteConfigList = git
                .remoteList()   //获取list
                .call();
        for (RemoteConfig x : remoteConfigList) {
            urlMap.put(x.getName(), x.getURIs().toString());   //获取名字，获取URL
        }
        System.out.println(urlMap);
    }

    /**
     * 添加远程地址
     *
     * @throws URISyntaxException
     * @throws GitAPIException
     */
    @Test
    public void gitAddRemote() throws URISyntaxException, GitAPIException {
        Git git = openRpo(localPath);

        RemoteAddCommand remoteAddCommand = git.remoteAdd();
        remoteAddCommand.setName("master");
        remoteAddCommand.setUri(new URIish("https://github.com/ssghyqr/software.git"));
        remoteAddCommand.call();

        RemoteAddCommand sshRemoteAddCommand = git.remoteAdd();
        sshRemoteAddCommand.setName("origin");
        sshRemoteAddCommand.setUri(new URIish("git@github.com:ssghyqr/software.git"));
        sshRemoteAddCommand.call();
    }

    /**
     * 删除远程地址
     *
     * @throws GitAPIException
     */
    @Test
    public void gitDelRemote() throws GitAPIException {
        Git git = openRpo(localPath);

        RemoteRemoveCommand remoteRemoveCommand = git.remoteRemove();
        remoteRemoveCommand.setName("master");
        remoteRemoveCommand.call();
    }

    /**
     * 推送命令
     * 使用的是ssh而不是https
     *
     * @throws GitAPIException
     */
    @Test
    public void gitPushOriginMaster() throws GitAPIException {
        Git git = openRpo(localPath);

        git.push().setRemote("origin").setTransportConfigCallback(transport -> {
            SshTransport sshTransport = (SshTransport) transport;
            sshTransport.setSshSessionFactory(sshSessionFactory);
        }).setRefSpecs(new RefSpec("master")).call();
    }

    /**
     * 测试远端拉取
     *
     * @throws GitAPIException
     */
    @Test
    public void gitPullOrigin() throws GitAPIException {
        Git git = openRpo(localPath);

        git.pull().setRemote("origin").setRemoteBranchName("master").setTransportConfigCallback(transport -> {
            SshTransport sshTransport = (SshTransport) transport;
            sshTransport.setSshSessionFactory(sshSessionFactory);
        }).call();
    }

    /**
     * 克隆远端仓库
     *
     * @throws GitAPIException
     */
    @Test
    public void gitClone() throws GitAPIException {
        File path = new File(localClonePath);
        if (path.exists()) {
            deleteDir(path.getPath());
        }

//        开始克隆
        Git git = Git.cloneRepository()
                .setURI(sshUri)
                .setDirectory(path)
                .setTransportConfigCallback(transport -> {
                    SshTransport sshTransport = (SshTransport) transport;
                    sshTransport.setSshSessionFactory(sshSessionFactory);
                })
                .setCloneSubmodules(true)
                .setBranch("master")
                .call();

        git.getRepository().close();
        git.close();
    }

    /**
     * 获取commit记录
     *
     * @throws IOException
     * @throws GitAPIException
     */
    @Test
    public void gitLogList() throws IOException, GitAPIException {
        Git git = openRpo(localPath);

        Iterable<RevCommit> logList = git.log().setMaxCount(5).call();
        for (RevCommit revCommit : logList) {
            System.out.println("logID: " + revCommit.getName()
                    + " logMessage: " + revCommit.getShortMessage());
        }
    }

    /**
     * 添加标签
     *
     * @throws GitAPIException
     * @throws IOException
     */
    @Test
    public void gitCreateTag() throws GitAPIException, IOException {
        Git git = openRpo(localPath);

        Iterable<RevCommit> logIter = git.log().setMaxCount(5).call();
        List<RevCommit> logList = new ArrayList<>();
        logIter.forEach(item -> {
            logList.add(item);
        });

//        默认拿取commit最晚的添加标签
        ObjectId id = git.getRepository().resolve(logList.get(0).getName());  //获取提交的ObjectID
        RevWalk walk = new RevWalk(git.getRepository());   //获取RevWalk对象
        RevCommit commit = walk.parseCommit(id);   //获取该commitID的RevCommit对象
        git.tag().setObjectId(commit)  //设置commit
                .setName("V1.0")  //设置tag名字
                .setMessage("test tag")  //设置tag注释
//                    .setAnnotated()  //是否为annotate
                .call();
    }

    /**
     * 获取全部tag
     *
     * @throws GitAPIException
     * @throws IOException
     */
    @Test
    public void gitTagList() throws GitAPIException, IOException {
        Git git = openRpo(localPath);

        List<Ref> refList = git.tagList().call();  //获取所有tag
        RevWalk walk = new RevWalk(git.getRepository());
        for (Ref ref : refList) {
            System.out.println("commitID:" + walk.parseCommit(ref.getObjectId()).getName());  // 通过ref获取objectID
            System.out.println("tagName:" + ref.getName());                                 // 然后通过walk获取commit对象再获																								   // 取commitId
        }
    }

    /**
     * 删除标签
     *
     * @throws GitAPIException
     */
    @Test
    public void gitDelTag() throws GitAPIException {
        Git git = openRpo(localPath);

        git.tagDelete().setTags("V1.0").call();
    }

    /**
     * 推送至远程
     *
     * @throws GitAPIException
     */
    @Test
    public void gitPushTag() throws GitAPIException {
        Git git = openRpo(localPath);

        git.push().setRemote("origin")
                .setPushTags()
                .setTransportConfigCallback(transport -> {
                    SshTransport sshTransport = (SshTransport) transport;
                    sshTransport.setSshSessionFactory(sshSessionFactory);
                })
                .call();
    }

    /**
     * 当前修改后与前一次commit版本对比
     * @throws GitAPIException
     */
    @Test
    public void gitDiff() throws GitAPIException {
        Git git = openRpo(localPath);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        git.diff()
                .setOutputStream(outputStream) //输出流  用outputStream，后面转成字符串
                .call();
        System.out.println(outputStream.toString());
    }

    /**
     * 本地仓库两个commit版本对比
     * @throws GitAPIException
     * @throws IOException
     */
    @Test
    public void gitDiffCommit() throws GitAPIException, IOException {
        Git git = openRpo(localPath);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        AbstractTreeIterator newTreeIter = prepareTreeParser(git.getRepository(), git.getRepository().resolve("HEAD").getName());
        AbstractTreeIterator oldTreeIter = prepareTreeParser(git.getRepository(), git.getRepository().resolve("HEAD^").getName());
        git.diff()
                .setNewTree(newTreeIter)  //设置源，不设置则默认工作区和历史最新commit版本比较
                .setOldTree(oldTreeIter)
//                    .setPathFilter(PathFilter.create(".txt"))  //设置过滤
                .setOutputStream(outputStream) //输出流  用outputStream，后面转成字符串
                .call();
        System.out.println(outputStream.toString());
    }

    /**
     * 默认回溯到前一个版本
     * @throws IOException
     * @throws GitAPIException
     */
    @Test
    public void gitReset() throws IOException, GitAPIException {
        Git git = openRpo(localPath);

        Iterable<RevCommit> logIter = git.log().setMaxCount(5).call();
        List<RevCommit> logList = new ArrayList<>();
        logIter.forEach(item -> {
            logList.add(item);
        });

        RevWalk walk = new RevWalk(git.getRepository());    //获取walk对象
//        默认回溯前一个版本
        ObjectId objectId = git.getRepository()
                .resolve(logList.get(1).getName());   //ObjectId对象
        RevCommit revCommit = walk.parseCommit(objectId);   //获取Revcommit对象
        String perVision = revCommit.getParent(0).getName();   //获取commit的身份名
        git.reset().setMode(ResetCommand.ResetType.HARD).setRef(perVision).call();  //设置参数
    }

    /**
     * 撤销，默认撤销前一个版本
     * @throws GitAPIException
     * @throws IOException
     */
    @Test
    public void gitRevert() throws GitAPIException, IOException {
        Git git = openRpo(localPath);

        Iterable<RevCommit> logIter = git.log().setMaxCount(5).call();
        List<RevCommit> logList = new ArrayList<>();
        logIter.forEach(item -> {
            logList.add(item);
        });

        RevWalk walk = new RevWalk(git.getRepository());
        ObjectId objectId = git.getRepository()
                .resolve(logList.get(1).getName());   //ObjectId对象
        RevCommit revCommit = walk.parseCommit(objectId);
        git.revert().include(revCommit).call();
    }
}
