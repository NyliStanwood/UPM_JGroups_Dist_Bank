1. Introducci´on
El objetivo de esta pr´actica es desarrollar una aplicaci´on distribuida que gestione la informaci´on de los
clientes de un banco, que garantice que se cumplan las propiedades de coherencia, disponibilidad y tolerancia
a fallos. La funcionalidad o los servicios son sencillos, para centrarse en los aspectos distribuidos del sistema.
El desarrollo se debe basar en la herramienta JGroups.
La gesti´on de los clientes se basa en caracter´ısticas simples:
La informaci´on que se debe almacenar para cada cliente incluye su n´umero de cuenta, nombre y saldo.
Gestionar una base de datos que contenga la informaci´on de los clientes.
Las operaciones o servicios que se deben proporcionar para gestionar la informaci´on de un cliente son:
crear un cliente, obtener su saldo y actualizar su saldo y eliminar un cliente.

Planteamiento de la aplicaci´on distribuida
La aplicaci´on estar´a compuesta de un conjunto de procesos que repliquen la base de datos de un banco.
La comunicaci´on entre los procesos debe ser ´unicamente mediante el paso de mensajes. De hecho, el sistema
deber´ıa funcionar correctamente, tanto si se ejecutan en un s´olo computador o en computadores remotos. El
sistema dispone de un conjunto de clientes que acceden a los servicios del banco y pueden interactuar con
cualquier proceso. La figura 1 ilustra este sistema distribuida.
El sistema proporciona los siguientes servicios para acceder al banco:
Put: crear un cliente al banco.
Get: obtener la informaci´on del cliente.
Update: actualizar el saldo del cliente.
Remove: eliminar el cliente del banco.
5. Requisitos del sistema
5.1. Requisitos funcionales
La aplicaci´on gestionar´a la informaci´on de los clientes del banco. El registro de cada cliente incluye su
n´umero de cuenta, su nombre y su saldo.
1. La aplicaci´on proporcionar´a servicios para la gesti´on de clientes: crear, leer, actualizar y eliminar.
2. La aplicaci´on se basar´a en un conjunto de servidores replicados, que se comunican mediante paso de
mensajes.
3. La aplicaci´on deber´a mantener un n´umero correcto de servidores (quorum). En caso de fallo de un
servidor, se deber´a iniciar un nuevo servidor y se transferir´a el estado del sistema, para mantener la
consistencia dentro del sistema.

Requisitos no funcionales
En el sistema distribuido se requieren los siguientes requisitos no funcionales:
1. Coherencia: la base de datos de los procesos replicados deben coincidir. La intuici´on de su correcci´on
consiste en que un cliente deber´ıa obtener las mismas respuestas al acceder a un sistema centralizado
o a cualquier proceso del sistema distribuido.
2. Tolerancia de fallos: el comportamiento del sistema distribuido deber´a funcionar correctamente,
cuando se producen un n´umero determinado de fallos de r´eplicas. Un cliente no deber´ıa ser consciente
de este problema del sistema.
Se supone que s´olo se van a tolerar fallos de procesos. Adem´as, se supone que el modo de fallos es
fallo-silencio (fail-silent). El sistema nunca dar´a valores err´oneos y deber´a disponer un detector de
fallos.
En caso de fallo de un proceso, se deber´ıa crear otra r´eplica, para mantener el n´umero de fallos
tolerables.
3. Disponibilidad: El sistema debe satisfacer un n´umero elevado de invocaciones de los servicios de los
clientes. Para ello, se deben a˜nadir procesos adicionales dinamicamente, para sustituir a algunos que
hayan fallado.
6. Un dise˜no del gestor de un banco distribuido.
En esta secci´on se propone un dise˜no del gestor distribuido. Se proporcionar´a una plantilla del sistema,
que se podr´a utilizar en el desarrollo de este laboratorio.
No es imprescindible usar este dise˜no. No es el ´unico, ni, muy probablemente, fuera el mejor. Es posible
e, incluso, deseable proponer alternativas propias.
6.1. Diagrama de clases
La figura 2 ilustra el diagrama de clases del gestor del banco. La clase MainBank inicia el programa. B´asi-
camente crea las clases requeridas, solicita invocaciones a los servicios y toma las medidas para ejecutarlas.

Las clases Menu y MenuCommands tratan de proporcionar un interfaz textual muy sencillo. No es necesario
modificar estas tres clases disponibles.
La funcionalidad de las clases adicionales es:
NodeJG: es la encargada de dirigir el comportamiento del sistema: a˜nade el proceso en el cluster del
grupo, crea los objetos necesarios, difunde los servicios invocados desde el men´u y procesa los mensajes
recibidos. Esta clase implementa el interfaz org.jgroups.Receiver.
ServicesBank: esta clase permite tratar las operaciones sobre el gestor. Cuando un usuario solicita
un servicio, se invoca el m´etodo adecuado de esta clase.
OperationsBank: esta clase permite encapsular toda la informaci´on de un operaci´on para su trans-
misi´on. Implementa el interfaz Serializable para su serializaci´on.
SendMessages: esta clase difunde la operaci´on a todo los procesos del grupo.
ProcessMsgBank: cuando se recibe el mensaje con una operaci´on, el m´etodo receive invoca a esta
clase para procersarlo.
ClientDB: representa la base de datos del banco de un proceso.
Client: encapsula la informaci´on personal de cada cliente del banco.
La documentaci´on (javadoc) de las clases se proporciona junto con el toda el material asociado a este
laboratorio. La figura 3 resuma el contenido de estas clases
Figura 2: Diagrama de clases del gestor distribuido de un banco.
6.2. Diagramas de secuencia
La figura 4 ilustra la difusi´on de una invocaci´on a un servicio, mediante el env´ıo de un mensaje a todos
los procesos del grupo.
La figura 5 ilustra la recepci´on y el procesamiento de un mensaje con una operaci´on sobre el gestor. Las
invocaciones a la clase Client no se han representado para simplificar la figura

Figura 3: Descripci´on de las clases del gestor distribuido de un banco.
Figura 4: Diagrama de secuencia: difundir un servicio del gestor distribuido de un banco.

Bibliograf´ıa
JGroups - A Toolkit for Reliable Messaging
Manual: Relieable group communicatgion with JGroups
Tutorial: Relieable group communicatgion with JGroups
JGroups javadoc