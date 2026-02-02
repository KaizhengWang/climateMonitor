Cesium.Ion.defaultAccessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiJjMzVjOTMwNS1lZGQyLTQxYTgtYmQ2Mi1lZjVlMjkxOWM3M2MiLCJpZCI6Mzg0MjU1LCJpYXQiOjE3Njk1ODAxNDN9.rsVCe43I-OGnuJhnCaeVk2B6t0k0PsnwSXFHPwr67w8";

// 创建 Viewer
const viewer = new Cesium.Viewer('cesiumContainer', {
    animation: false,
    timeline: false,
    baseLayerPicker: false,
    navigationHelpButton: false,
    geocoder: false,
    homeButton: false,
    sceneModePicker: false,
    fullscreenButton: true
});

// 澳门全景初始视角
viewer.camera.setView({
    destination: Cesium.Cartesian3.fromDegrees(113.5530, 22.1240, 30000.0),
    orientation: {
        heading: Cesium.Math.toRadians(0),
        pitch: Cesium.Math.toRadians(-83),
        roll: 0.0
    }
});

// 调整滚轮缩放灵敏度
const controller = viewer.scene.screenSpaceCameraController;
controller.zoomEventMultiplier = 0.05; // 缩放更慢
controller.inertiaSpin = 0.0;         // 关闭旋转惯性
controller.inertiaTranslate = 0.0;    // 关闭平移惯性
controller.inertiaZoom = 0.5;         // 轻度缩放惯性

// ---------- 加载澳门边界 GeoJSON ----------
Cesium.GeoJsonDataSource.load('/geojson/macau_boundaryv2.geojson', {
    stroke: Cesium.Color.RED,               // 边框红色
    fill: Cesium.Color.fromAlpha(Cesium.Color.WHITE, 0.1), // 半透明白填充
    strokeWidth: 2,
    clampToGround: true
}).then(dataSource => {
    viewer.dataSources.add(dataSource);
});

// 创建站点实体
const stationEntities = {};
stations.forEach(station => {
    const labelText = station.displayName || station.name;
const entity = viewer.entities.add({
    id: station.id,
    name: station.displayName || station.name,
    position: Cesium.Cartesian3.fromDegrees(station.lon, station.lat, 50),
    point: {
        pixelSize: 16, // 可视大小
        color: Cesium.Color.fromCssColorString('#E74C3C')
    },
    // 用一个透明的更大点增加点击区域
    ellipse: {
        semiMinorAxis: 25,  // 扩大判定范围，单位：像素
        semiMajorAxis: 25,
        material: Cesium.Color.TRANSPARENT,
        outline: false
    },
    label: {
        text: station.displayName || station.name,
        font: '22px "Microsoft YaHei", bold',
        fillColor: Cesium.Color.BLACK,
        outlineColor: Cesium.Color.WHITE,
        outlineWidth: 3,
        style: Cesium.LabelStyle.FILL_AND_OUTLINE,
        verticalOrigin: Cesium.VerticalOrigin.BOTTOM,
        pixelOffset: new Cesium.Cartesian2(0, -25)
    }
    });
    stationEntities[station.id] = entity;
});

// 点击事件：只处理 stations 中的实体
const handler = new Cesium.ScreenSpaceEventHandler(viewer.scene.canvas);
handler.setInputAction(click => {
    const picked = viewer.scene.pick(click.position);

    if (picked && picked.id) {
        const clickedStation = stations.find(s => s.id === picked.id.id);
        if (clickedStation) {
            updateInfoPanel(clickedStation.id); // 更新面板
            return;
        }
    }

    // 点击到其他地方或非站点实体，一律关闭面板
    closeInfoPanel();

}, Cesium.ScreenSpaceEventType.LEFT_CLICK);
