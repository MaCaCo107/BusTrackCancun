const mongoose = require('mongoose');

// Un esquema para guardar cada punto de la línea roja en el mapa
const PuntoCoordenadaSchema = new mongoose.Schema({
    latitud: { type: Number, required: true },
    longitud: { type: Number, required: true }
});

const RutaSchema = new mongoose.Schema({
    nombre: { type: String, required: true, unique: true }, 
    origenNombre: { type: String, required: true },       
    destinoNombre: { type: String, required: true },      
    
   
    trazado: [PuntoCoordenadaSchema], 
    
    fechaCreacion: { type: Date, default: Date.now }
});



module.exports = mongoose.model('Ruta', RutaSchema);