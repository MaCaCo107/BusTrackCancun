const mongoose = require('mongoose');

const ReporteSchema = new mongoose.Schema({
    titulo: { type: String, required: true },
    descripcion: { type: String, required: true },
    
    nombreUsuario: { type: String, required: true }, 
    fecha: { type: Date, default: Date.now },     
   
    ubicacion: {
        type: { type: String, default: 'Point' },
        coordenadas: { type: [Number], required: true } 
    }
});


module.exports = mongoose.model('Reporte', ReporteSchema, 'reportes');