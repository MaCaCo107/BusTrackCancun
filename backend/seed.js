const mongoose = require('mongoose');
const Ruta = require('./models/Ruta');

mongoose.connect('mongodb://localhost:27017/busTrackDB');

const cargarRuta = async () => {
    const nuevaRuta = new Ruta({
        nombre: "Ruta 26 - Jardines del Sur - Rancho Viejo",
        origenNombre: "Jardines del Sur",
        destinoNombre: "Rancho Viejo",
        trazado: [
            {latitud: 21.110291, longitud: -86.883737},
            {latitud: 21.211437, longitud: -86.843036}
        ]
    });
    await nuevaRuta.save();
    console.log("Ruta cargada exitosamente");
    process.exit();
};
cargarRuta();