package com.c3stones.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.c3stones.client.BaseConfig;
import com.c3stones.client.Dockers;
import com.c3stones.client.Kubes;
import com.c3stones.client.pod.NacosPod;
import com.c3stones.client.pod.NginxPod;
import com.c3stones.client.pod.NginxPod2;
import com.c3stones.common.Response;
import com.c3stones.entity.*;
import com.c3stones.util.KubeUtils;
import com.c3stones.util.OpenFileUtils;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 部署应用 Controller
 * 
 * @author CL
 *
 */
@Controller
@Slf4j
@RequestMapping(value = "deploy")
public class DeployController  extends BaseConfig {



	@Autowired
	private Kubes kubes;

	@Autowired
	private Dockers dockers;

	@Autowired
	private NacosPod nacosPod ;

    /***
     * labels
     */
    private  static  String LABELS_KEY = "app";

	/*@Value("${harbor.image.prefix}")
      private String imagePrefix;*/

	/**
	 * nfs 名称
	 */
/*	@Value("${nfs.storage.className}")
	private String nfsStorageClassName;*/

	@Autowired
	private  NginxPod nginxPod;

	@Autowired
	private NginxPod2 nginxPod2;

	@Value("${pod.namespace.prefix}")
	private String podNamespacePrefix;

	@Value("${pod.app.prefix}")
	private String podAppPrefix;

	@Value("${pod.nginx.prefix}")
	private String podNginxPrefix;

	/**
	 * 查询列表
	 *
	 * @return
	 */
	@RequestMapping(value = "list")
	public String list() {
		return "pages/deploy/list";
	}




	/**
	 * 检验
	 *
	 * @param namespace
	 * @return false 可能， true 已存在
	 */
	@RequestMapping(value = "check")
	@ResponseBody
	public Response<Boolean> checkUserName(@NotNull String namespace) {
		return Response.success(!kubes.checkNamespace(namespace));
	}

	/**
	 * 新增
	 *
	 * @return
	 */
	@RequestMapping(value = "add")
	public String add() {
		return "pages/deploy/add";
	}


	/**
	 * 新增
	 *
	 * @return
	 */
	@RequestMapping(value = "addNginx")
	public String addNginx() {
		return "pages/deploy/addNginx";
	}

	/**
	 * deployPod
	 *
	 * @return
	 */
	@RequestMapping(value = "deployPod")
	public String deployPod( Model model,String image) {
        model.addAttribute("image", image);
        model.addAttribute("namespace", kubes.getNamespace());
        return "pages/deploy/deployPod";
    }

	/**
	 * 多个部署应用
	 * @param model
	 * @param image
	 * @return
	 */
	@RequestMapping(value = "deployMultiPod")
	public String deployMultiPod( Model model,String image) {
        model.addAttribute("image", image);
        model.addAttribute("namespace", kubes.getNamespace());
        return "pages/deploy/deployMultiPod";
    }
		/**
	 * deployPod
	 *
	 * @return
	 */
	@RequestMapping(value = "deployNginx")
	public String deployNginx( Model model) {
		model.addAttribute("namespace",kubes.getNamespace());
		return "pages/deploy/deployNginx";
	}

	@RequestMapping(value = "updatePage")
	public String update(String deployname,String replicas,String image,String namespace, Model model) {
		model.addAttribute("deployname",deployname);
		model.addAttribute("replicas",replicas);
		model.addAttribute("image",image);
		model.addAttribute("namespace",namespace);
		return "pages/pod/update";
	}

	/**
	 * deployPod
	 *
	 * @return
	 */
	@RequestMapping(value = "getNamespace")
	@ResponseBody
	public List<Namespaces> getNamespace() {
		return kubes.getNamespace();
	}

	/**
	 * getNacos
	 *
	 * @return
	 */
	@RequestMapping(value = "getNacos")
	@ResponseBody
	public List<Pods> getNacos() {
		return nacosPod.getNacos();
	}

	@RequestMapping(value = "update")
	@ResponseBody
	public Response<Boolean> update(String deployname ,String namespace ,String image,Integer replicas) {
		Assert.notNull(deployname, "deployname不能为空");
		Assert.notNull(namespace, "namespace不能为空");
		Assert.notNull(image, "image不能为空");
		Assert.notNull(replicas, "replicas不能为空");
		System.out.println("image===>"+image);
		System.out.println("deployname===>"+deployname);
		System.out.println("namespace===>"+namespace);
		System.out.println("replicas===>"+replicas);
		KubernetesClient kubeclinet = kubes.getKubeclinet();
		kubeclinet.apps().deployments().inNamespace(namespace).withName(deployname).edit()
				.editSpec().withReplicas(replicas).editTemplate().editSpec().editContainer(0)
				.withImage(harborImagePrefix+"/"+image)
				.endContainer().endSpec().endTemplate().endSpec().done();
		return Response.success(true);
	}

	@RequestMapping(value = "delDeployment")
	@ResponseBody
	public Response<Boolean> delDeployment(String deployname ,String namespace) {
		Assert.notNull(deployname, "deployname不能为空");
		Assert.notNull(namespace, "namespace不能为空");
		System.out.println("deployname===>"+deployname);
		System.out.println("namespace===>"+namespace);
		KubernetesClient kubeclinet = kubes.getKubeclinet();
        Deployment deployment = kubeclinet.apps().deployments().inNamespace(namespace).withName(deployname).get();
        Boolean delete = kubeclinet.apps().deployments().inNamespace(namespace).withName(deployname).delete();
        kubes.deleteService(namespace,deployment.getSpec().getTemplate().getMetadata().getLabels().get(LABELS_KEY));

		kubeclinet.configMaps().inNamespace(namespace).withName(deployname).delete();

		List<Volume> volumes = deployment.getSpec().getTemplate().getSpec().getVolumes();
		if(volumes != null  && volumes.size() > 0){
			volumes.forEach(a->{
				kubeclinet.persistentVolumeClaims().inNamespace(namespace).withName(a.getName()).delete();
			});
		}
		return Response.success(true);
	}

	/**
	 * pod
	 *
	 * @return
	 */
	@RequestMapping(value = "pod")
	@ResponseBody
	public Response<Boolean> pod(String namespace, String image,String podName,String nacos,Integer port,Integer nodePort,Integer replicas,
	String nacosNamespace,Integer nfs ,Integer memoryXmx,Integer memoryXms
	) {
		System.out.println(namespace);
		System.out.println(podName);
		System.out.println(image);
		String[] split = nacos.split("---");
		System.out.println(nacos);
		System.out.println(split[2]);
		System.out.println(port);
		System.out.println(nodePort);
		System.out.println(nacosNamespace);
		System.out.println(replicas);
		System.out.println("nfs==="+nfs);
		log.info("部署  namespace : {},image:{},nacos:{},port:{},nodePort:{}",namespace,image,nacos,port,nodePort);
		Assert.notNull(namespace, "namespace不能为空");
		Assert.notNull(image, "image不能为空");
		Assert.notNull(split[2], "nacos不能为空");
		Assert.notNull(nfs, "nfs不能为空");
		Assert.notNull(memoryXmx, "memoryXmx不能为空");
		Assert.notNull(memoryXms, "memoryXms不能为空");
		String randomPortName = KubeUtils.randomPortName();
		if(kubes.checkSvc(nodePort)){
			return Response.error("端口已存在");
		}

		if(kubes.checkdeployname(namespace,podAppPrefix+podName)){
			return Response.error("服务已存在");
		}
		try {

		image = harborImagePrefix + "/"+image;
		String pvcLogs = namespace+podName+"-logs";
			kubes.createPVC(pvcLogs,namespace,nfsStorageClassName,12);
			if(nfs == 1){
					kubes.createPVC(namespace+podName,namespace,nfsStorageClassName,20);
					if(kubes.createDeployment(namespace,namespace,podName,replicas,image,port,randomPortName,split[2],
							nacosNamespace,namespace+podName, memoryXmx, memoryXms,pvcLogs)){
						if(nodePort != null  && port != null) {
							kubes.createService(namespace,randomPortName,port,nodePort);
						}
						return Response.success(true);
					}
				}else{
					if(kubes.createDeployment(namespace,namespace,podName,replicas,image,port,randomPortName,split[2],
							nacosNamespace, memoryXmx, memoryXms,pvcLogs)){
						if(nodePort != null  && port != null) {
							kubes.createService(namespace,randomPortName,port,nodePort);
						}
						return Response.success(true);
					}
				}
		}catch (Exception e){
			kubes.deleteDeployments(namespace,podAppPrefix+podName);
			kubes.deleteService(namespace,randomPortName);
			kubes.deletePvc(namespace,namespace+podName);
			e.printStackTrace();
		}

		return Response.error("失败");
	}
	/**
	 *  部署多个应用
	 *
	 * @return
	 */
	@RequestMapping(value = "mepoyMultiPod")
	@ResponseBody
	public Response<JSONObject> mepoyMultiPod(String namespace, String images,String nacos,Integer replicas,
	String nacosNamespace,Integer nfs ,Integer memoryXmx,Integer memoryXms
	) {
		Assert.notNull(memoryXmx, "memoryXmx不能为空");
		Assert.notNull(memoryXms, "memoryXms不能为空");
		System.out.println(namespace);
		System.out.println(images);
		String[] split = nacos.split("---");
		System.out.println(nacos);
		System.out.println(split[2]);
		System.out.println(nacosNamespace);
		System.out.println(replicas);
		System.out.println("nfs==="+nfs);
		String randomPortName = KubeUtils.randomPortName();
		String[] imageList = images.split(",");
		String podName = null;
		int exist = 0;
		int error = 0;
		int success = 0;
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("exist",0);
		jsonObject.put("existPod",0);
		jsonObject.put("error",0);
		jsonObject.put("success",0);
		jsonObject.put("errorPod",0);
		for (int i = 0 ; i < imageList.length ; i++){
			try {
				String image = imageList[i];
				 podName = image.split("/")[1].replace(":", "-").replace(".", "-");
				String pvcLogs = namespace+podName+"-logs";
				kubes.createPVC(pvcLogs,namespace,nfsStorageClassName,12);
				if(kubes.checkdeployname(namespace,podAppPrefix+podName)){
					jsonObject.put("exist",++exist);
					//jsonObject.put("existPod",podName);
				}else {
					image = harborImagePrefix + "/"+image;
					if(nfs == 1){
						kubes.createPVC(namespace+podName,namespace,nfsStorageClassName,20);
						kubes.createDeployment(namespace,namespace,podName,replicas,image,null,randomPortName,split[2],nacosNamespace,
								namespace+podName, memoryXmx, memoryXms,pvcLogs);
					}else{
						kubes.createDeployment(namespace,namespace,podName,replicas,image,null,randomPortName,split[2],nacosNamespace, memoryXmx, memoryXms,pvcLogs);
					}
					jsonObject.put("success",++success);
				}
			}catch (Exception e){
				jsonObject.put("error",++error);
				//jsonObject.put("errorPod",podName);
				kubes.deleteDeployments(namespace,podAppPrefix+podName);
				kubes.deleteService(namespace,randomPortName);
				kubes.deletePvc(namespace,namespace+podName);
				e.printStackTrace();
			}
		}
		System.out.println(jsonObject.get("error"));
		System.out.println(jsonObject.get("success"));
		System.out.println(jsonObject.get("exist"));
		return Response.success(jsonObject);
	}


	/**
	 * pod
	 *
	 * @return
	 */
	@RequestMapping(value = "pod/nginx")
	@ResponseBody
	public Response<Boolean> deployNginx(String namespace, String image,Integer port,Integer nodePort,
										 MultipartFile  conf) {
		System.out.println("namespace=="+namespace);
		System.out.println("image=="+image);
		System.out.println("port=="+port);
		System.out.println("nodePort=="+nodePort);
		System.out.println("conf=="+conf);
		log.info("部署  namespace : {},image:{},nacos:{},port:{},nodePort:{}",namespace,image,conf,port,nodePort);
		Assert.notNull(namespace, "namespace不能为空");
		Assert.notNull(image, "image不能为空");
		Assert.notNull(conf, "file不能为空");
		String podName =image.substring(image.lastIndexOf("/")+1,image.length()).replaceAll(":","-").replaceAll("\\.","-");
		System.out.println("podName="+podName);
		if(kubes.checkSvc(nodePort)){
			return Response.error("端口已存在");
		}
		if(kubes.checkdeployname(namespace,podNginxPrefix+podName)){
			return Response.error("服务已存在");
		}
		image = harborImagePrefix + "/"+image;
		try {
            nginxPod2.configMap(namespace,podName,podName,OpenFileUtils.fileConverString(conf));
			nginxPod2.createDeployment(namespace,podName,podName,image,port,podName);
			nginxPod2.createService(namespace,podName,port,nodePort);
        }catch (Exception e){
		    e.printStackTrace();
            nginxPod.delete(namespace,podName);
			return Response.error("失败");
        }
		return Response.success("OK");
	}

	/**
	 * pod
	 *
	 * @return
	 */
	@RequestMapping(value = "getNacosNamespace")
	@ResponseBody
	public Response<List<NacosEntity>> getNacosNamespace(String ip, Integer port) {
		List<NacosEntity> namespace = nacosPod.getNamespace(ip, port);
		return Response.success(namespace);
	}

	@RequestMapping(value = "getDeployment")
	@ResponseBody
	public Response<Pages<Deployments>> getDeployment(String namespace) {
		List<Deployments> deployments = new ArrayList<>();
		List<Deployment> items = kubes.getKubeclinet().apps().deployments().list().getItems();
		items.forEach(a->{
			if((a.getMetadata().getNamespace().startsWith(podNamespacePrefix) ||
					a.getMetadata().getNamespace().startsWith(podNginxPrefix))
					&& StringUtils.isNotBlank(namespace)&&a.getMetadata().getNamespace().startsWith(namespace)){
				ObjectMeta metadata = a.getMetadata();
				String name = metadata.getName();
				Integer replicas = a.getSpec().getReplicas();
				String creationTimestamp = metadata.getCreationTimestamp();
				String image = a.getSpec().getTemplate().getSpec().getContainers().get(0).getImage();
				String substring = image.substring(image.indexOf("/") + 1, image.length());
				Deployments deployment = new Deployments(name,replicas,KubeUtils.StringFormatDate(creationTimestamp),metadata.getNamespace(),substring);
				deployments.add(deployment);
			}
			if(a.getMetadata().getNamespace().startsWith(podNamespacePrefix) && StringUtils.isBlank(namespace)){
				ObjectMeta metadata = a.getMetadata();
				String name = metadata.getName();
				Integer replicas = a.getSpec().getReplicas();
				String creationTimestamp = metadata.getCreationTimestamp();
				String image = a.getSpec().getTemplate().getSpec().getContainers().get(0).getImage();
				String substring = image.substring(image.indexOf("/") + 1, image.length());
				Deployments deployment = new Deployments(name,replicas,KubeUtils.StringFormatDate(creationTimestamp),metadata.getNamespace(),substring);
				deployments.add(deployment);

			}

		});
		Pages page = new Pages();
		page.setRecords(deployments);
		page.setTotal(deployments.size());
		return Response.success(page);
	}



	/**
	 * 上传文件
	 *
	 * @param file
	 * @return
	 */
	@RequestMapping("/upload")
	@ResponseBody
	public Response<HarborImage> upload(MultipartFile[] file ,String version
	) {
		Assert.notNull(version, "version不能为空");
		Assert.notNull(file, "file不能为空");
		log.info("制作镜像 file :{} , version : {}",file,version);
		//String homeDir = null;
		List<String> errorList = new ArrayList<>();
		List<String> successList = new ArrayList<>();
		HarborImage harborImage = new HarborImage();
		try {

					Arrays.stream(file).forEach(f->{
						String 	homeDir = null;
						String originalFilename = null;
						try {
							originalFilename = f.getOriginalFilename();
							String name = KubeUtils.randomPortName();
							homeDir = Dockers.getHomeDir()+File.separator+name;
							new File(homeDir).mkdirs();
							log.info("制作目录--> homeDir : {}",homeDir);
							multipartFileToFile(f,homeDir);
							dockers.writeDockerfile(originalFilename,homeDir);
							dockers.upload(homeDir, originalFilename.substring(0,originalFilename.indexOf(".")),version);
							successList.add(originalFilename);
						} catch (Exception e) {
							errorList.add(originalFilename);
							e.printStackTrace();
						}finally {
							harborImage.setErrorData(errorList);
							harborImage.setSuccessData(successList);
							removeTempFile(homeDir);
						}
					});

		} catch (Exception e) {
			e.printStackTrace();
			return Response.error("失败");
		}
		log.info("制作镜像成功");
		return Response.success(harborImage);

	}
	/**
	 * 上传文件
	 *
	 * @param file
	 * @return
	 */
	@RequestMapping("/upload/nginx")
	@ResponseBody
	public Response<Pages<HarborImage>>uploadNginx(MultipartFile file ,String version
	) {
		log.info("制作镜像 file :{} , version : {}",file,version);
		Assert.notNull(version, "version不能为空");
		Assert.notNull(file, "file不能为空");
		String homeDir = null;
		try {
			String name = KubeUtils.randomPortName();
			 homeDir = Dockers.getHomeDir()+File.separator+name;
			 new File(homeDir).mkdirs();
			log.info("制作目录--> homeDir : {}",homeDir);
		    multipartFileToFile(file,homeDir);
			String originalFilename = file.getOriginalFilename();
			dockers.writeNginxDockerfile(originalFilename,homeDir);
			dockers.upload(homeDir, originalFilename.substring(0,originalFilename.indexOf(".")),version);
		} catch (Exception e) {
			e.printStackTrace();
            return Response.error("失败");
        }finally {
			try {
				removeTempFile(homeDir);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		log.info("制作镜像成功");
		return Response.success("OK");

	}


	/**
	 * MultipartFile 转 File
	 *
	 * @param file
	 * @throws Exception
	 */
	public static File multipartFileToFile(MultipartFile file ,String homeDir) throws Exception {
		File toFile = null;
		if (file.equals("") || file.getSize() <= 0) {
			file = null;
		} else {
			InputStream ins = null;
			ins = file.getInputStream();
			toFile = new File(homeDir,file.getOriginalFilename());
			inputStreamToFile(ins, toFile);
			ins.close();
		}
		return toFile;
	}

	//获取流文件
	private static void inputStreamToFile(InputStream ins, File file) {
		try {
			OutputStream os = new FileOutputStream(file);
			int bytesRead = 0;
			byte[] buffer = new byte[8192];
			while ((bytesRead = ins.read(buffer, 0, 8192)) != -1) {
				os.write(buffer, 0, bytesRead);
			}
			os.close();
			ins.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 删除本地临时文件
	 *
	 * @param file
	 */
	public static void delteTempFile(File file) {
		if (file != null) {
			File del = new File(file.toURI());
			del.delete();
		}

	}

	/**
	 * 删除文件与目录
	 * @param filePath
	 */
	public static void removeTempFile(String... filePath) {
		if(filePath.length > 0){
			for (int i = 0; i < filePath.length; i++){
				log.info("删除目录 : {} "+filePath[i]);
				File tempFilePath=new File(filePath[i]);
				if(tempFilePath.exists() && tempFilePath.isDirectory()){
					for (File chunk : tempFilePath.listFiles()) {
						if(chunk.isDirectory()){
							removeTempFile(chunk.getPath());
						}else{
							System.gc();//启动jvm垃圾回收
							chunk.delete();
						}
					}
				}
				System.gc();//启动jvm垃圾回收
				tempFilePath.delete();

			}
		}
	}

	public static void main(String[] args) {
         removeTempFile("C:\\Users\\Administrator\\.kube-deployment\\.docker\\pod-380347");
	}

}