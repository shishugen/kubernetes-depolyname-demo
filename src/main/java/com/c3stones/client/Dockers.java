package com.c3stones.client;

import com.c3stones.entity.Config;
import com.c3stones.util.UnRar;
import com.c3stones.util.XYFileUtils;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.DockerCmdExecFactory;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.SearchItem;
import com.github.dockerjava.api.model.Version;
import com.github.dockerjava.core.*;
import com.github.dockerjava.core.command.BuildImageResultCallback;
import com.github.dockerjava.core.command.PushImageResultCallback;
import com.github.dockerjava.core.command.WaitContainerResultCallback;
import com.github.dockerjava.core.dockerfile.Dockerfile;
import com.github.dockerjava.core.util.CompressArchiveUtil;
import com.github.dockerjava.jaxrs.JerseyDockerCmdExecFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang.StringUtils;
import org.ho.yaml.Yaml;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import sun.tools.jar.resources.jar;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * @ClassName: Dockers
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/1/18 9:42
 */
@Slf4j
@Component
public class Dockers extends BaseConfig{

 //   private static final String DEFAULT_HOST_I P = "10.49.0.9";

 /*   @Value("${docker.host}")
    private String dockerHost;*/

 /*   @Value("${docker.port:2375}")
    private String dockerPort;*/

  //  private static final String DEFAULT_HOST_PORT = "2375";

/*    @Value("${docker.file.dir}")
    private  String dockerFileDir;*/

    private  String DEFAULT_FILE_DIRECTORY ;

    //仓库地址
   // private static String REGISTR = "10.49.0.9/application/";

/*    @Value("${harbor.image.prefix}")
    private String imagePrefix;

    @Value("${harbor.image.project.name}")
    private String projectName;*/

    @Value("${nginx.image}")
    private String nginxImage;

    @Value("${python.image}")
    private String pythonImage;
/*
    @Value("${harbor.password}")
    private String harborPassword;

    @Value("${harbor.user}")
    private String harborUser;

    @Value("${harbor.url}")
    private String harborUrl;*/

    private static String separator = System.getProperty("line.separator");

    public  DockerClient getDockerClient() {
        DockerClient dockerClient = null;
        log.info("获取连接:{}", dockerClient);
            synchronized (Dockers.class) {
                String   serverIp = "";
                String homeConfigFile = Dockers.getHomeConfigFile();
                File file = new File(homeConfigFile);
                File[] files = file.listFiles();
                if(files != null && files.length > 2){
                    for (File f :files){
                        if(f.isDirectory()){
                             serverIp = f.getName();
                        }
                    }
                }
                return   getDockerClient(homeConfigFile, serverIp);
            }
    }

    public  DockerClient getDockerClient(String confPath, String serverIp) {
        DockerClient dockerClient = null;
        log.info("获取连接:{}", dockerClient);
     // isWindows();
            synchronized (Dockers.class) {
                log.info("配置文件路径===" + confPath);
                log.info("IP加端口===" + serverIp+":"+dockerPort);
                DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().withDockerTlsVerify(true)
                        .withCustomSslConfig(new LocalDirectorySSLConfig(confPath))
                        .withDockerHost("tcp://" + serverIp + ":" + dockerPort)
                      //   .withDockerCertPath(confPath)
                      //  .withDockerConfig(confPath )
                     //   .withRegistryUrl(harborUrl)
                      //  .withRegistryUsername(harborUser)
                    //    .withRegistryPassword(harborPassword)
                        .build();
                dockerClient = DockerClientBuilder.getInstance(config).build();
                Version exec = dockerClient.versionCmd().exec();
                int size = dockerClient.listImagesCmd().exec().size();
                System.out.println("docker Version"+exec.getVersion());
                System.out.println("docker images size : "+size);
            }
        return dockerClient;
    }

/*
    public  void isWindows() {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        if (isWindows) {
            DEFAULT_FILE_DIRECTORY = dockerFileDir;
        } else {
            DEFAULT_FILE_DIRECTORY = File.separator + "home";
        }
    }*/

    public  File writeDockerfile(String jarPath ,String homeDir) throws IOException {
        File file = new File(homeDir + File.separator + "Dockerfile");
        file.createNewFile();
        String conifName = "bootstrap.yaml";
        writeNacosConfFile(conifName,homeDir);
        FileOutputStream outputStream = new FileOutputStream(file);//形参里面可追加true参数，表示在原有文件末尾追加信息
        String jar = "ADD  " + jarPath + " /app.jar";
        String conf = "COPY  "+conifName+"  /";
      //  String libs = "COPY  libs  /libs";
        outputStream.write("FROM openjdk:8-jdk-alpine".getBytes());
        outputStream.write(separator.getBytes());
        outputStream.write(jar.getBytes());
       // outputStream.write(separator.getBytes());
      //  outputStream.write(libs.getBytes());
        outputStream.write(separator.getBytes());
        outputStream.write(conf.getBytes());
        outputStream.write(separator.getBytes());


     /*   String confFile = "jar app.jar  BOOT-INF/classes/application.yml";
        outputStream.write(confFile.getBytes());
        outputStream.write(separator.getBytes());

        String env = "ENV PORT = ";
        outputStream.write(env.getBytes());
        outputStream.write(separator.getBytes());
*/


       // outputStream.write("RUN rm /etc/localtime && ln -s /usr/share/zoneinfo/Asia/Shanghai /etc/localtime".getBytes());
      //  outputStream.write(separator.getBytes());

        outputStream.write("RUN apk add --no-cache tzdata".getBytes());
        outputStream.write(separator.getBytes());
        outputStream.write("ENV TZ=Asia/Shanghai".getBytes());
        outputStream.write(separator.getBytes());

        outputStream.write("ENV Jvm_Xms \"-Xms512m\"".getBytes());
        outputStream.write(separator.getBytes());

        outputStream.write("ENV Jvm_Xmx \"-Xmx1024m\"".getBytes());
        outputStream.write(separator.getBytes());

       outputStream.write("ENTRYPOINT [\"java\",\"-jar\",\"-Xms512m\",\"-Xmx1024m\", \"/app.jar\"]".getBytes());
      //  outputStream.write("ENTRYPOINT [\"java\",\"$JVM\",\"-Djava.security.egd=file:/dev/./urandom\",\"-jar\",\"/app.jar\"]".getBytes());

       // outputStream.write("ENTRYPOINT [\"java\",\"-jar\",\"/app.jar\"]".getBytes());
      //  outputStream.write("ENTRYPOINT [\"java\",\"-Dloader.path=libs/\",\"-jar\",\"/app.jar\"]".getBytes());
        return file;
    }
  public  File writeNginxDockerfile(String fileName,String homeDir) throws Exception {
        File file = new File(homeDir + File.separator + "Dockerfile");
        file.createNewFile();

        FileOutputStream outputStream = new FileOutputStream(file);//形参里面可追加true参数，表示在原有文件末尾追加信息

        outputStream.write(("FROM "+harborImageEnvPrefix+nginxImage).getBytes());
        outputStream.write(separator.getBytes());


     //  UnRar.unRar(new File(homeDir+File.separator+fileName),homeDir);
     //  String name = fileName.substring(0, fileName.lastIndexOf("."));
        String s1 = "COPY " + fileName + "  /"+fileName;
        outputStream.write(s1.getBytes());
        outputStream.write(separator.getBytes());

       outputStream.write("RUN rm /etc/localtime && ln -s /usr/share/zoneinfo/Asia/Shanghai /etc/localtime".getBytes());
        outputStream.write(separator.getBytes());

        String unzip = "RUN unzip "+ fileName;
        outputStream.write(unzip.getBytes());
        outputStream.write(separator.getBytes());


        outputStream.write("EXPOSE 80".getBytes());
        outputStream.write(separator.getBytes());
        return file;
    }

    /**
     *
     * @param fileName
     * @param homeDir
     * @return
     * @throws Exception
     */
    public  File writePythonDockerfile(String fileName,String homeDir) throws Exception {
        File file = new File(homeDir + File.separator + "Dockerfile");
        file.createNewFile();

        FileOutputStream outputStream = new FileOutputStream(file);//形参里面可追加true参数，表示在原有文件末尾追加信息


        outputStream.write(("FROM "+harborImageEnvPrefix+pythonImage).getBytes());
        outputStream.write(separator.getBytes());


        outputStream.write(("RUN pip install Flask py2neo==2021.1.1 jieba sklearn gunicorn gevent").getBytes());
        outputStream.write(separator.getBytes());

        String s1 = "COPY " + fileName + "  /"+fileName;
        outputStream.write(s1.getBytes());
        outputStream.write(separator.getBytes());

       outputStream.write("RUN rm /etc/localtime && ln -s /usr/share/zoneinfo/Asia/Shanghai /etc/localtime".getBytes());
        outputStream.write(separator.getBytes());

        String unzip = "RUN unzip  "+fileName;
        outputStream.write(unzip.getBytes());
        outputStream.write(separator.getBytes());

        String name = fileName.substring(0, fileName.lastIndexOf("."));
        String s = "CMD [\"gunicorn\",\"--chdir\",\""+name+"\", \"app:app\", \"-c\", \"./" + name + "/gunicorn.conf.py\"," +
                "\"--log-level=debug\",\"--access-logfile=-\",\"--error-logfile=-\"]";
        outputStream.write(s.getBytes());
        outputStream.write(separator.getBytes());
        return file;
    }





    public  File writeNacosConfFile(String conifName, String homeDir ) throws IOException {
        File file = new File(homeDir + File.separator + conifName);
        file.createNewFile();
        FileOutputStream outputStream = new FileOutputStream(file);//形参里面可追加true参数，表示在原有文件末尾追加信息
        String conf = "spring:\n" +
                "    cloud:\n" +
                "        nacos:\n" +
                "            config:\n" +
                "                namespace: ${NAMESPACE}\n" +
                "                server-addr: ${NACOS_IP}:${NACOS_PORT}\n" +
                "            discovery:\n" +
                "                namespace: ${NAMESPACE}\n" +
                "                server-addr: ${NACOS_IP}:${NACOS_PORT}\n"+
                "management:\n" +
                "    server:\n" +
                "      port: 9000\n" +
                "    endpoints:\n" +
                "        web:\n" +
                "            exposure:\n" +
                "                include: '*'\n" +
                "            base-path: /";
        outputStream.write(conf.getBytes());
        return file;
    }


    public void upload(String baseDir ,String image,String tag) throws InterruptedException {
        log.info("baseDir : {} ,image : {}",baseDir,image);
        DockerClient dockerClient = getDockerClient();
        Dockers dockers = new Dockers();
        String imageName = harborImagePrefix+"/"+harborImageProjectName+"/"+image;
        HashSet<String> objects = new HashSet<>(1);
        objects.add(imageName+":"+tag);
        log.info("start buildImage……imageName  : {}:{} ",imageName,tag);
        dockerClient.buildImageCmd(new File(baseDir)).withNoCache(true)
                .withTags(objects).exec(new BuildImageResultCallback()).awaitImageId(8, TimeUnit.MINUTES);
        log.info("end buildImage……imageName  : {}:{} ",imageName,tag);

        AuthConfig authConfig = new AuthConfig();
        authConfig.withUsername(harborUser);
        authConfig.withPassword(harborPassword);
        authConfig.withRegistryAddress(harborUrl);
        log.info("harborUrl ……harborUser……harborPassword …… :{} {}:{} ",harborUrl,harborUser,harborPassword);
        dockerClient.pushImageCmd(imageName).withAuthConfig(authConfig)
                .withTag(tag)
                .exec(new PushImageResultCallback())
                .awaitCompletion(3, TimeUnit.MINUTES);
        log.info("pushImage……imageName  : {}:{} ",imageName,tag);
        log.info("imageName : {}","上传完成 "+imageName);

    }

    /**
     * docker 配置文件夹
     * @return
     */
    public static String getHomeConfigDir() {
        String dir = System.getProperty("user.home") +File.separator + ".kube-deployment"+ File.separator + ".docker"+ File.separator+"configs";
        if(!new File(dir).exists()){
            new File(dir).mkdirs();
        }
        return dir;
    }

    /**
     * docker 当前 配置
     * @return
     */
    public static String getHomeConfigFile() {
        String dir = System.getProperty("user.home") + File.separator + ".kube-deployment"+ File.separator + ".docker"+ File.separator +"config";
        if(!new File(dir).exists()){
            new File(dir).mkdirs();
        }
        return dir;
    }




    public static String getHomeDir() {
        String dir = System.getProperty("user.home") + File.separator + ".kube-deployment"+ File.separator + ".docker";
        if(!new File(dir).exists()){
            new File(dir).mkdirs();
        }
        return dir;

    }


    public static void main(String[] args) throws InterruptedException, Exception {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("tcp://192.168.103.236:2375")
                .withCustomSslConfig(new LocalDirectorySSLConfig("E:\\dockerfile\\192.168.103.236"))
                .build();
        DockerClient dockerClient = DockerClientBuilder.getInstance(config).build();
        List<Image> exec = dockerClient.listImagesCmd().exec();
    /*    List<SearchItem> exec1 = dockerClient.searchImagesCmd("ssgssg").exec();
        exec1.stream().forEach(a->{
            System.out.println("1==="+a);
            System.out.println("2=="+a.getDescription());
            System.out.println("3==="+a.getName());
            System.out.println("4==="+a.getStarCount());
        });*/
        exec.stream().skip(4).limit(1).filter(a->a.getRepoTags() != null && a.getRepoTags().length > 0)
                 .filter(new Predicate<Image>() {
                     @Override
                     public boolean test(Image test) {
                       //  String gender = test.getGender();
                         return "".equals("");
                     }
                 }).collect(Collectors.toList());

          /*      .forEach(a->{
            System.out.println(a.getId());
            Arrays.stream(a.getRepoTags()).forEach(t->{
                String projectName = "";
                if(t.contains("/")){
                     projectName = t.split("/")[0];
                }else{
                     projectName = t.split(":")[0];
                }
             //   System.out.println(t +"    "+projectName);
            });
        });*/
      //  dockerClient.removeImageCmd("ade6db6b752468088e729846a4fb7707a2611a91c6521d2f5802dce82d8e8974").exec();
    }






}
