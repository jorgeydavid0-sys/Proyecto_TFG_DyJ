import re

with open(r'C:\Users\david\Git_Repository\_odt_extracted\content.xml', 'r', encoding='utf-8') as f:
    raw = f.read()

def p(text):
    return f'<text:p text:style-name="Normal">{text}</text:p>'

def pb():
    return '<text:p text:style-name="Normal"/>'

sec5 = (
    p('Para el desarrollo de la aplicacion interactiva Escuela Interactiva 2D se han empleado las siguientes tecnologias, seleccionadas en funcion de su adecuacion al problema, la experiencia previa del equipo y su caracter de codigo abierto.') +
    pb() +
    p('Java SE (JDK 17). Lenguaje de programacion principal empleado tanto en el modulo cliente como en el servidor. Fue seleccionado por su portabilidad entre sistemas operativos mediante la maquina virtual (JVM), su robustez en entornos multihilo y los conocimientos consolidados del equipo durante el ciclo formativo DAM. Toda la logica de la aplicacion, incluyendo la gestion de conexiones, la logica de negocio y la interfaz grafica, esta implementada en Java.') +
    pb() +
    p('Java Swing y Graphics2D. Biblioteca grafica incluida en el JDK utilizada para la interfaz visual del cliente. Swing proporciona los componentes de ventana, panel y dialogo, mientras que la API Graphics2D permite el renderizado programatico del mapa 2D completo: tiles del escenario, personajes, animaciones de movimiento y elementos interactivos. Al generarse los graficos por codigo, la aplicacion no depende de activos de imagen externos.') +
    pb() +
    p('Sockets TCP (ServerSocket / Socket). La comunicacion en tiempo real entre clientes y servidor se gestiona mediante la API estandar de red de Java (java.net). El servidor instancia un ServerSocket que acepta conexiones entrantes de forma indefinida, asignando un hilo independiente a cada cliente. Este modelo garantiza la sincronizacion de posiciones, zonas y mensajes de chat entre todos los usuarios conectados.') +
    pb() +
    p('Java Threading (Thread / ConcurrentHashMap). El servidor gestiona multiples conexiones simultaneas mediante hilos Java (java.lang.Thread). Cada cliente conectado es atendido por un ClientHandler dedicado, permitiendo la concurrencia sin bloqueos. El estado compartido del mapa se protege mediante colecciones concurrentes (ConcurrentHashMap), evitando condiciones de carrera.') +
    pb() +
    p('PostgreSQL 16. Sistema de gestion de bases de datos relacional de codigo abierto empleado para la persistencia de todos los datos del sistema: usuarios, credenciales cifradas, notas, horarios, tablon de anuncios y menu del comedor. Fue seleccionado por su fiabilidad, su soporte completo del estandar SQL, su compatibilidad nativa con el driver JDBC oficial y su uso extendido en entornos de produccion empresarial y academica.') +
    pb() +
    p('JDBC, Driver postgresql-42.7.4.jar. La conectividad entre el servidor Java y PostgreSQL se realiza mediante el driver oficial JDBC de PostgreSQL (version 42.7.4). Permite ejecutar consultas SQL parametrizadas desde Java, gestionando el ciclo de vida de las conexiones y protegiendo frente a inyecciones SQL.') +
    pb() +
    p('bcrypt. Algoritmo de hash unidireccional utilizado para el almacenamiento seguro de contrasenas. Las credenciales de los usuarios nunca se guardan en texto plano; se aplica bcrypt con sal (salt) aleatoria para generar un hash irreversible que se compara en cada proceso de autenticacion, en cumplimiento del RGPD.') +
    pb() +
    p('Git y GitHub. Sistema de control de versiones distribuido empleado para gestionar el codigo fuente del proyecto de forma colaborativa. Cada integrante trabajo en su propia rama de desarrollo, integrando los avances mediante fusiones al repositorio compartido en GitHub, que tambien sirvio como repositorio de documentacion e hitos del proyecto.') +
    pb() +
    p('Visual Studio Code. Entorno de desarrollo integrado (IDE) empleado por ambos integrantes durante la fase de codificacion, por su compatibilidad con Java, su integracion nativa con Git y su ecosistema de extensiones para depuracion y desarrollo colaborativo.') +
    pb()
)

sec7 = (
    p('El desarrollo de la Escuela Interactiva 2D ha permitido alcanzar los objetivos planteados al inicio del proyecto, materializando una aplicacion de escritorio que demuestra con rigor tecnico la viabilidad de aplicar principios de gamificacion al entorno educativo como alternativa a los portales web institucionales y aulas virtuales tradicionales.') +
    pb() +
    p('Desde el punto de vista tecnico, la integracion de una arquitectura cliente-servidor basada en sockets TCP con Java, un motor grafico 2D construido sobre Swing y Graphics2D, y una base de datos relacional PostgreSQL supuso un reto real de ingenieria de software que ambos integrantes afrontaron y resolvieron de forma progresiva. La implementacion del modelo multihilo para la gestion de conexiones concurrentes resulto el componente mas exigente del proyecto y, al mismo tiempo, el que mayor aprendizaje tecnico aporto al equipo.') +
    pb() +
    p('Una de las decisiones mas relevantes del proceso fue el cambio de la propuesta tecnologica inicial, que contemplaba un stack web basado en Python y herramientas de generacion automatica de codigo, hacia una implementacion nativa en Java con Swing y sockets TCP. Esta decision resulto determinante para garantizar la coherencia tecnica del producto final con los conocimientos adquiridos en el ciclo formativo DAM y para producir un software mas robusto y controlado.') +
    pb() +
    p('En cuanto a los objetivos funcionales, la aplicacion permite gestionar sesiones de usuario con diferenciacion de tres roles (alumno, profesor y administrador), navegar por un mapa escolar 2D con transiciones entre zonas, visualizar informacion academica personalizada mediante interaccion con elementos del mapa, y comunicarse en tiempo real mediante un chat multijugador. Todos estos objetivos han sido implementados y verificados mediante las pruebas funcionales descritas en el apartado 4.3.') +
    pb() +
    p('Como reflexion critica, se reconoce que el estilo visual, aunque coherente y funcional, quedo condicionado por los plazos disponibles, optando por graficos generados programaticamente frente a recursos de imagen externos. Esta decision, acertada desde el punto de vista de la viabilidad temporal, limita el impacto visual inicial en usuarios de mayor edad, aspecto identificado como la principal linea de mejora futura.') +
    pb() +
    p('En conclusion, el proyecto ha demostrado que es posible construir una plataforma educativa interactiva tecnicamente solvente con los recursos y conocimientos propios de un equipo de dos alumnos de DAM, aportando una solucion innovadora, original y funcionalmente completa a un problema real del entorno escolar.') +
    pb()
)

sec9 = (
    p('Oracle Corporation. (2024). Java SE 17 Documentation. Oracle Technology Network. Recuperado de https://docs.oracle.com/en/java/javase/17/') +
    pb() +
    p('Oracle Corporation. (2024). Java SE Tutorial: Creating a GUI with Swing. Oracle Technology Network. Recuperado de https://docs.oracle.com/javase/tutorial/uiswing/') +
    pb() +
    p('Oracle Corporation. (2024). Java SE Tutorial: All About Sockets. Oracle Technology Network. Recuperado de https://docs.oracle.com/javase/tutorial/networking/sockets/') +
    pb() +
    p('The PostgreSQL Global Development Group. (2024). PostgreSQL 16 Documentation. Recuperado de https://www.postgresql.org/docs/16/') +
    pb() +
    p('The PostgreSQL Global Development Group. (2024). PostgreSQL JDBC Driver Documentation (v42.7.4). Recuperado de https://jdbc.postgresql.org/documentation/') +
    pb() +
    p('Horstmann, C. S. (2021). Core Java, Volume I: Fundamentals (12.a ed.). Pearson. ISBN 978-0137673629.') +
    pb() +
    p('Schildt, H. (2022). Java: The Complete Reference (13.a ed.). McGraw-Hill Education. ISBN 978-1260463422.') +
    pb() +
    p('Goetz, B., Peierls, T., Bloch, J., Bowbeer, J., Holmes, D., y Lea, D. (2006). Java Concurrency in Practice. Addison-Wesley Professional. ISBN 978-0321349606.') +
    pb() +
    p('Deterding, S., Dixon, D., Khaled, R., y Nacke, L. (2011). From game design elements to gamefulness: defining gamification. Proceedings of the 15th International Academic MindTrek Conference (pp. 9-15). ACM. https://doi.org/10.1145/2181037.2181040') +
    pb() +
    p('Prensky, M. (2001). Digital Game-Based Learning. McGraw-Hill. ISBN 978-0071454100.') +
    pb() +
    p('Parlamento Europeo y del Consejo de la Union Europea. (2016). Reglamento (UE) 2016/679 relativo a la proteccion de las personas fisicas en lo que respecta al tratamiento de datos personales (RGPD). Diario Oficial de la Union Europea, L 119, 1-88.') +
    pb() +
    p('Jefatura del Estado. (2018). Ley Organica 3/2018, de 5 de diciembre, de Proteccion de Datos Personales y garantia de los derechos digitales (LOPDGDD). Boletin Oficial del Estado, num. 294, 119788-119857.') +
    pb() +
    p('Agencia Espanola de Proteccion de Datos (AEPD). (2024). Guia para el cumplimiento del deber de informar. Recuperado de https://www.aepd.es/') +
    pb() +
    p('Pressman, R. S., y Maxim, B. R. (2020). Ingenieria del software: un enfoque practico (9.a ed.). McGraw-Hill. ISBN 978-1260548013.') +
    pb() +
    p('GitHub, Inc. (2024). GitHub Documentation: Managing your work on GitHub. Recuperado de https://docs.github.com/') +
    pb()
)

raw = raw.replace(
    '<text:p text:style-name="P779"/><text:p text:style-name="P780">6. DESARROLLO REALIZADO',
    sec5 + '<text:p text:style-name="P780">6. DESARROLLO REALIZADO'
)
raw = raw.replace(
    '<text:p text:style-name="P785"/><text:p text:style-name="P786"/><text:p text:style-name="P787">8. L',
    sec7 + '<text:p text:style-name="P787">8. L'
)
raw = raw.replace(
    '<text:p text:style-name="P792"/><text:p text:style-name="P793">10. ANEXOS',
    sec9 + '<text:p text:style-name="P793">10. ANEXOS'
)

with open(r'C:\Users\david\Git_Repository\_odt_extracted\content.xml', 'w', encoding='utf-8') as f:
    f.write(raw)

print(f"Done. File size: {len(raw)}")
