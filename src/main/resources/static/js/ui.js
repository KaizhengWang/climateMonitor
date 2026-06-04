const infoPanel = document.getElementById('infoPanel');

function updateInfoPanel(stationId) {
    const station = stations.find(s => s.id === stationId);
    if (!station) return;

    document.getElementById('stationName').textContent = station.name;
    infoPanel.style.display = 'block';

    fetch('/api/weather/now/' + encodeURIComponent(stationId))
        .then(res => res.json())
        .then(data => {
            document.getElementById('tempValue').textContent = val(data.temperature, '°C');
            document.getElementById('humidityValue').textContent = val(data.humidity, '%');
            document.getElementById('windSpeedValue').textContent = val(data.windSpeed10min, 'km/h');
            document.getElementById('windDirValue').textContent = data.windDirection || '--';
            document.getElementById('rainHourValue').textContent = val(data.rain1hour, 'mm');
        })
        .catch(() => {
            ['tempValue','humidityValue','windSpeedValue','windDirValue','rainHourValue']
                .forEach(id => document.getElementById(id).textContent = '--');
        });
}

function val(v, unit) {
    return v != null ? v + ' ' + unit : '--';
}

function closeInfoPanel() {
    infoPanel.style.display = 'none';
}

document.addEventListener('keydown', e => {
    if (e.key === 'Escape') closeInfoPanel();
});
