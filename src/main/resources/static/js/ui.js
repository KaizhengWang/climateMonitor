const infoPanel = document.getElementById('infoPanel');
const stationNameEl = document.getElementById('stationName');
const tempEl = document.getElementById('tempValue');
const humidityEl = document.getElementById('humidityValue');
const windSpeedEl = document.getElementById('windSpeedValue');
const windDirEl = document.getElementById('windDirValue');
const rainHourEl = document.getElementById('rainHourValue');

function updateInfoPanel(stationId) {
    infoPanel.style.display = 'block';

    const station = stations.find(s => s.id === stationId);
    stationNameEl.textContent = station.displayName || station.name;

    fetch('/api/weather/macau')
        .then(res => res.json())
        .then(data => {
            tempEl.textContent = data.temperature[station.name] || 'N/A';
            humidityEl.textContent = data.humidity[station.name] || 'N/A';
            windSpeedEl.textContent = data.windSpeed[station.name] || 'N/A';
            windDirEl.textContent = data.windDirection[station.name] || 'N/A';
            rainHourEl.textContent = data.rainHour[station.name] || 'N/A';
        })
        .catch(err => {
            console.error(err);
            tempEl.textContent = 'N/A';
            humidityEl.textContent = 'N/A';
            windSpeedEl.textContent = 'N/A';
            windDirEl.textContent = 'N/A';
            rainHourEl.textContent = 'N/A';
        });
}

function closeInfoPanel() {
    infoPanel.style.display = 'none';
}

// 支持 ESC 关闭面板
document.addEventListener('keydown', e => {
    if (e.key === 'Escape') closeInfoPanel();
});

//const infoPanel = document.getElementById('infoPanel');
//const stationNameEl = document.getElementById('stationName');
//const tempEl = document.getElementById('tempValue');
//const humidityEl = document.getElementById('humidityValue');
//
//function updateInfoPanel(stationId) {
//    // 显示面板
//    infoPanel.style.display = 'block';
//
//    // 设置站点名
//    const station = stations.find(s => s.id === stationId);
//    stationNameEl.textContent = station.displayName || station.name;
//
//    // 拉取后端数据
//    fetch('/api/weather/macau')
//        .then(res => res.json())
//        .then(data => {
//            const temp = data.temperature[station.name] || 'N/A';
//            const hum = data.humidity[station.name] || 'N/A';
//            tempEl.textContent = temp;
//            humidityEl.textContent = hum;
//        })
//        .catch(err => {
//            console.error(err);
//            tempEl.textContent = 'N/A';
//            humidityEl.textContent = 'N/A';
//        });
//
//}
//
//function closeInfoPanel() {
//    infoPanel.style.display = 'none';
//
//
//
//// 支持 ESC 关闭面板
//document.addEventListener('keydown', e => {
//    if (e.key === 'Escape') closeInfoPanel();
//});
