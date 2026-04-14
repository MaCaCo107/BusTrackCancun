const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const Ruta = require('./models/Ruta'); 

const app = express();

// Middleware
app.use(express.json()); 
app.use(cors());         


const MONGO_URI = process.env.MONGO_URI || 'mongodb://127.0.0.1:27017/busTrackDB';

mongoose.connect(MONGO_URI, { 
    useNewUrlParser: true, 
    useUnifiedTopology: true 
})
.then(() => console.log(`Conectado a MongoDB en: ${MONGO_URI}`))
.catch(err => console.error('Error de conexión:', err));


// Esquema para el Registro de Usuarios
const UsuarioSchema = new mongoose.Schema({
    nombre: { type: String, unique: true, required: true },
    correo: { type: String, unique: true, required: true }, 
    contrasena: String,
    fechaRegistro: { type: Date, default: Date.now }
});
const Usuario = mongoose.model('Usuario', UsuarioSchema);




app.get('/', (req, res) => {
    res.send({
        status: "En línea",
        proyecto: "BusTrack Cancún API",
        mensaje: "El servidor está respondiendo correctamente"
    });
});


app.post('/api/usuarios', async (req, res) => {
try {
        const { nombre, correo, contrasena } = req.body;
        
       
        const existe = await Usuario.findOne({ $or: [{ correo }, { nombre }] });
        if (existe) {
            return res.status(400).send({ error: "El usuario o correo ya existen." });
        }

        const nuevoUsuario = new Usuario({ nombre, correo, contrasena });
        await nuevoUsuario.save();
        
        console.log(`Usuario registrado: ${nombre}`);
        res.status(201).send({ mensaje: 'Cuenta creada con éxito' });
    } catch (error) {
        res.status(500).send({ error: "Error al registrar usuario" });
    }
});

// Ruta para Iniciar Sesión (Login)
app.post('/api/login', async (req, res) => {
    try {
        const { correo, contrasena } = req.body;

        const usuarioEncontrado = await Usuario.findOne({ correo: correo });

        if (!usuarioEncontrado) {
            return res.status(404).send({ error: "El correo no está registrado." });
        }

        if (usuarioEncontrado.contrasena !== contrasena) {
            return res.status(401).send({ error: "Contraseña incorrecta." });
        }

        
        console.log(`Login exitoso: ${usuarioEncontrado.nombre}`);
        res.status(200).send({ 
            mensaje: "¡Bienvenido!",
            usuario: {
                nombre: usuarioEncontrado.nombre,
                correo: usuarioEncontrado.correo
            }
        });

    } catch (error) {
        console.error("Error en el proceso de login:", error);
        res.status(500).send({ error: "Error interno del servidor." });
    }
});

app.post('/api/reportar', async (req, res) => {
    try {
        const nuevoReporte = new Reporte(req.body);
        await nuevoReporte.save();
        res.status(201).send({ mensaje: 'Reporte de ruta guardado' });
    } catch (error) {
        res.status(400).send({ error: 'Error al enviar reporte' });
    }
});



//Iniciar Servidor
const PORT = 3000;
app.listen(PORT, '0.0.0.0', () => {
    console.log(`Servidor corriendo  http://localhost:${PORT}`);
    console.log(`App : http://***.1**.1.**:${PORT}/api/usuarios`);
});

// Obtener todas las rutas disponibles
app.get('/api/rutas', async (req, res) => {
    try {
       
        const rutasValidas = await Ruta.find();
        res.status(200).json(rutasValidas);
    } catch (error) {
        res.status(500).json({ error: "Error al obtener las rutas" });
    }
});

// Obtener el trazado específico de UNA ruta por su ID
app.get('/api/rutas/:id/trazado', async (req, res) => {
    try {
        const rutaCompleta = await Ruta.findById(req.params.id);
        if (!rutaCompleta) return res.status(404).json({ error: "Ruta no encontrada" });
        
       
        res.status(200).json({
            _id: rutaCompleta._id,
            nombre: rutaCompleta.nombre,
            trazado: rutaCompleta.trazado 
        });
    } catch (error) {
        res.status(500).json({ error: "Error al obtener el trazado" });
    }
});

app.get('/api/rutas/buscar', async (req, res) => {
    try {
        const query = req.query.q; 
        if (!query) return res.json([]);
        const sugerencias = await Ruta.find({
            nombre: { $regex: query, $options: 'i' }
        }).limit(5); 
        res.status(200).json(sugerencias);
    } catch (error) {
        res.status(500).json({ error: "Error en la búsqueda" });
    }
});

const Reporte = require('./models/Reporte'); 


app.get('/api/reportes', async (req, res) => {
    console.time('Consulta_Reportes');
    try {
        const comentarios = await Reporte.find();
        res.status(200).json(comentarios);
    } catch (error) {
        res.status(500).json({ error: "Error retrieving comments" });
    }
});

app.post('/api/reportes', async (req, res) => {
    try {
        
        const { titulo, descripcion, latitud, longitud, nombreUsuario } = req.body;

        if (!titulo || !descripcion || !nombreUsuario) {
            return res.status(400).json({ error: "Título, descripción y usuario son obligatorios" });
        }

        const nuevoReporte = new Reporte({
            titulo,
            descripcion,
            nombreUsuario: nombreUsuario, // Guardar el usuario real de la sesión
            ubicacion: {
                coordenadas: [longitud, latitud]
            }
        });

        await nuevoReporte.save();
        res.status(201).json({ mensaje: "¡Comentario publicado!", reporte: nuevoReporte });

    } catch (error) {
        res.status(500).json({ error: "Error al publicar comentario" });
    }
});