const mongoose = require('mongoose');
const Reporte = require('./models/Reporte'); 

mongoose.connect('mongodb://127.0.0.1:27017/busTrackDB');

const generarDatos = async (cantidad) => {
    console.time(`Tiempo de inserción para ${cantidad} registros`);
    const reportes = [];

    for (let i = 0; i < cantidad; i++) {
        reportes.push({
            titulo: `Reporte de Prueba ${i}`,
            descripcion: `Esta es una descripción de prueba para evaluar el rendimiento del sistema número ${i}.`,
            nombreUsuario: "Tester_Pro",
            fecha: new Date(),
            ubicacion: {
                latitud: 21.16 + (Math.random() * 0.05), 
                longitud: -86.85 + (Math.random() * 0.05)
            }
        });
    }

    try {
        await Reporte.insertMany(reportes);
        console.log(`¡Éxito! Se insertaron ${cantidad} reportes.`);
        console.timeEnd(`Tiempo de inserción para ${cantidad} registros`);
        process.exit();
    } catch (error) {
        console.error("Error en la prueba:", error);
        process.exit(1);
    }
};


generarDatos(50);