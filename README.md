# Hazelcast and Oracle Streaming Demo
This project creates a simple [Hazelcast streaming](https://docs.hazelcast.com/hazelcast/5.3/pipelines/overview#hide-nav) job that reads changes from Oracle 23c free via [CDC](https://docs.hazelcast.com/hazelcast/5.3/integrate/cdc-connectors#hide-nav) and processes them. The project is deployed on Google Kubernetes Engine (GKE) using Ansible. 

The GKE cluster and all the deployments on it are done by your laptop using Ansible. After creating the GKE cluster, ansible deploys [Oracle 23c free](https://www.oracle.com/in/database/free/) and Hazelcast. For details on how each is deployed you can have a look at the inndividual roles in the `k8s/roles` directory.

Finally the playbook deploys the Hazelcast streaming job which starts streaming changes from `customers` table and inserts the records into a Hazelcast map. The job is deployed using Hazelcast CLC.

### Pipeline
The Hazelcast pipeline that recieves the changes from Oracle and inserts them into a Hazelcast map is defined in `CDCOracle.java`.

## GKE
### Pre-requisites
1. [Service Account](https://developers.google.com/identity/protocols/oauth2/service-account#creatinganaccount) with permissions to create GKE. The service account must be downloaded as a JSON file and located in `~/.gcp/credentials.json`
2. Ansible installed in local machine. Used to orchastrate the deployment.
3. kubectl installed in local machine. Used to interact with the GKE cluster.
4. Helm installed in local machine. Used to install Oracle and Hazelcast operator.
5. [Hazelcast CLC](https://docs.hazelcast.com/clc/latest/install-clc) installed in local machine. Used to deploy jobs to the Hazelcast Platform.
6. Maven installed in local machine.
7. Hazelcast License key present in file `~/hazelcast/hazelcast.license` in local machine. You may try Hazelcast for free by [registering](https://hazelcast.com/get-started/) and getting a license key.
8. Google Auth plugin installed pip - `pip3 install google-auth` in local machine
9. Install `gloud` via `brew install google-cloud-sdk` and `gke-gcloud-auth-plugin` via `gcloud components install gke-gcloud-auth-plugin` in local machine
10. A repository in [GCP Artifact Registry](https://cloud.google.com/artifact-registry/docs/docker/store-docker-container-images#linux) with 
name mapping to variable `repository_id` in region `asia-south1`. Ansible will push the docker image to this repository. Various parameters can be modified via the var files in `roles`. 

### Steps
In general the ansible playbook, `deploy.yaml` does the following:
1. Creates a GKE cluster including VPC, subnets and nodes. See `gke` role.
1. Creates a Oracle database. See `odb23cfree` role.
1. Create self-signed certificates for Hazelcast. See `hz` role.
1. Creates two Hazelcast clusters with WAN replication and a Management Center. TLS is enabled. See `hz` role.
1. Creates a Hazelcast streaming job. See `cdc-job` role.

1. Start Docker on the laptop and execute `ansible-playbook k8s/deploy.yaml`.
2. You can run individual tasks by using `tags`.
For example, following commands will create the GKE cluster, populate the database, configure ssl certificates, deploy Hazelcast clusters and start Management Center.
The next command will deploy the pipeline and the next one starts producing the transactions.
    1. `ansible-playbook k8s/deploy.yaml --tags="gke,oracle,ssl,hz,deb-con,job"`
    2. `ansible-playbook k8s/deploy.yaml --tags="job"`
    3. `ansible-playbook k8s/deploy.yaml --tags="oracle"` 
3. You may check the cluster on MC on `http://<EXTERNAL-IP>:8080` where `EXTERNAL-IP` is the external IP of the service `hazelcast-mc` service. Run `kubectl get svc` to get the IP.

### Undo
To shutdown the cluster execute `ansible-playbook k8s/undeploy.yaml` and delete the cluster from GCP console.


## Technologies of interest
1. [Hazelcast Platform](https://hazelcast.com) - Hazelcast Platform is a unified real-time data platform that enables companies to act instantly on streaming data. It combines high-performance stream processing capabilities with a built-in fast data store to automate, streamline, and enhance business-critical processes and applications. 
1. Google Jib. Jib is a Java containerizer from Google that lets Java developers build containers using the Java tools they know. It is a Maven plugin that builds Docker and OCI images for your Java applications and is available as plugins for Maven and Gradle.
## References
1. Oracle [registry](https://container-registry.oracle.com/ords/f?p=113:4:4481791090117:::4:P4_REPOSITORY,AI_REPOSITORY,AI_REPOSITORY_NAME,P4_REPOSITORY_NAME,P4_EULA_ID,P4_BUSINESS_AREA_ID:1863,1863,Oracle%20Database%20Free,Oracle%20Database%20Free,1,0&cs=32e9rTDJLrb2i9p_OnflfiHakooJB6m7nNI8AtdAGksYU7q6zoaKeKfvCAfukxg0gi-1j8cAQewuxTXvFgEIVRQ)
1. Debezium example with [Oracle](https://debezium.io/blog/2022/09/30/debezium-oracle-series-part-1/) part 2, [Part 2](https://debezium.io/blog/2022/10/06/ and debezium-oracle-series-part-2/) [Part 3](https://debezium.io/blog/2023/06/29/debezium-oracle-series-part-3/)
1. [Debezium Oracle connector](https://debezium.io/documentation/reference/1.9/connectors/oracle.html)
1. [Hazelcast Platform CDC](https://docs.hazelcast.com/hazelcast/5.3/pipelines/cdc-overview)
## TODO
1. Oracle setup script should be automated
1. Filter DDL events from Oracle in pipeline
1. Create a `Customer` domain object that use json serialization for transforming from Oracle to POJO and then compact serialization before storing in the database
1. Turn on [singnaling](https://debezium.io/documentation/reference/1.9/configuration/signalling.html)
