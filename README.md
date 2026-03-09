# 📱 KALI Termux Android App

Aplicación Android para conectar y controlar Kali Linux a través de SSH con una interfaz de terminal integrada.

## 🎯 Características

- **Conexión SSH Segura**: Conecta a tu servidor Kali Linux mediante SSH
- **Terminal Integrada**: Terminal completa con soporte para comandos y navegación
- **Interfaz Hacker**: Diseño con estilo underground y ASCII art
- **Gestión de Credenciales**: Opción para recordar contraseña de forma segura
- **Soporte Completo**: Teclas especiales, Ctrl+C, navegación con flechas

## 📋 Requisitos

- Android 5.0+ (API Level 21)
- Conexión a red local
- Servidor Kali Linux con SSH habilitado
- Credenciales de acceso SSH

## 🚀 Instalación

1. Clona este repositorio:
```bash
git clone https://github.com/yeifer125/app-termux-kali.git
```

2. Abre el proyecto en Android Studio
3. Configura tu dispositivo o emulador
4. Compila y ejecuta la aplicación

## ⚙️ Configuración

1. **Configuración del Servidor**:
   - Asegúrate que SSH esté habilitado en tu Kali Linux
   - Configura las credenciales de usuario
   - Verifica la conexión de red

2. **Configuración de la App**:
   - Ingresa la contraseña SSH cuando se solicite
   - Opcional: Activa "Recordar" para guardar credenciales

## 🔧 Uso

1. **Conexión**:
   - Abre la aplicación
   - Ingresa tu contraseña SSH
   - Presiona "CONNECT"

2. **Terminal**:
   - Usa la terminal como en cualquier sistema Linux
   - Navega con las flechas del teclado
   - Usa Ctrl+C para interrumpir procesos
   - Copia y pega texto fácilmente

## 🛠️ Tecnologías

- **Kotlin**: Lenguaje principal
- **Android SDK**: Plataforma móvil
- **Termux Terminal**: Componente de terminal
- **JSch**: Librería SSH
- **Material Design**: Interfaz de usuario

## 📁 Estructura del Proyecto

```
app/
├── src/main/
│   ├── java/com/kalilinux/kali/
│   │   └── MainActivity.kt         # Actividad principal
│   ├── res/
│   │   ├── layout/                 # Diseños XML
│   │   ├── drawable/              # Recursos gráficos
│   │   └── values/                # Strings y temas
│   └── assets/
│       ├── id_rsa_kali           # Clave SSH privada
│       └── id_rsa_kali.pub       # Clave SSH pública
└── build.gradle.kts              # Configuración de build
```

## 🔐 Seguridad

- Las contraseñas se guardan en SharedPreferences (opcional)
- Conexión SSH cifrada
- Soporte para autenticación con clave SSH

## 🐛 Problemas Conocidos

- La conexión puede fallar si el servidor SSH no está accesible
- Algunos caracteres especiales pueden no mostrarse correctamente
- Requiere configuración manual de la IP del servidor

## 🤝 Contribuciones

Las contribuciones son bienvenidas. Por favor:

1. Fork este repositorio
2. Crea una rama (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## 📄 Licencia

Este proyecto está bajo la Licencia MIT - mira el archivo [LICENSE](LICENSE) para detalles.

## 👤 Autor

**yeifer125** - *Desarrollador Principal*

## 🙏 Agradecimientos

- Termux por la librería de terminal
- JSch por la implementación SSH
- Comunidad Kali Linux

---

⚠️ **ADVERTENCIA**: Esta herramienta es para propósitos educativos y de testing de seguridad. Úsala responsablemente y solo en sistemas que tengas permiso para acceder.
