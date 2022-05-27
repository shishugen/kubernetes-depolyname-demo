package com.c3stones.entity;

import lombok.Data;
import lombok.ToString;

/**
 * @ClassName: K8sNode
 * @Description: TODO
 * @Author: stone
 * @Date: 2022/5/20 13:40
 */
@Data
@ToString
public class K8sNode {

    public K8sNode() { }
    public K8sNode(String nodeIp, Integer port) {
        this.nodeIp = nodeIp;
        this.port = port;
    }

    private String nodeName;
    private String nodeIp;
    private Integer port;
    private String ports;
    private String status;
    private String k8sVersion;
    private String dockerVersion;
    private String osImage;
    private boolean isMaster;

    @Data
    public class Node{
        private String nodeIp;
        private String ports;


        public Node() {

        }
        public Node(String nodeIp) {
            this.nodeIp = nodeIp;
        }
        public Node(String nodeIp, String ports) {
            this.nodeIp = nodeIp;
            this.ports = ports;
        }

        @Override
        public String toString() {
            return "节点IP=" + nodeIp +
                    ":" + ports
                    ;
        }
    }


}
