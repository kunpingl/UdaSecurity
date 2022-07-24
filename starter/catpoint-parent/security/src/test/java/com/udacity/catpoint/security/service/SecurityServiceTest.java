package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {
    private Sensor sensor;
    private SecurityService securityService;

    @Mock
    SecurityRepository securityRepository;
    @Mock
    ImageService imageService;

    private Set<Sensor> getSensors(boolean isActive) {
        int NUM_OF_SENSORS = 10;
        Set<Sensor> sensors = new HashSet<>();
        for (int i = 0; i < NUM_OF_SENSORS; i++) sensors.add(new Sensor(UUID.randomUUID().toString(), SensorType.DOOR));
        for (Sensor sensor : sensors) sensor.setActive(isActive);
        return sensors;
    }

    @BeforeEach
    void init() {
        sensor = new Sensor(UUID.randomUUID().toString(), SensorType.DOOR);
        securityService = new SecurityService(securityRepository, imageService);
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void alarmStatus_alarmArmedAndSensorActivated_pendingAlarmStatus(ArmingStatus armingStatus) {
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void alarmStatus_alarmArmedAndSensorActivatedAndAlarmPending_alarmAlarmStatus(ArmingStatus armingStatus) {
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void alarmStatus_alarmPendingAndSensorsInactive_noAlarms() {
        Set<Sensor> sensors = getSensors(false);
        when(securityRepository.getSensors()).thenReturn(sensors);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        Sensor first = sensors.iterator().next();
        first.setActive(true);
        securityService.changeSensorActivationStatus(first, false);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    void alarmStatus_alarmActiveAndStatusChanging_notAffected() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));

        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    void alarmStatus_sensorActivateAndPending_alarmState() {
        sensor.setActive(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void alarmStatus_sensorDeactivate_notAffected() {
        sensor.setActive(false);

        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    void alarmStatus_catDetectedAndSystemArmed_alarmStatus() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);

        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void alarmStatus_catNotDetected_noAlarmStatus() {
        Set<Sensor> sensors = getSensors(false);
        when(securityRepository.getSensors()).thenReturn(sensors);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);

        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    void alarmStatus_alarmDisarmed_alarmStatus() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void alarmStatus_systemArmed_resetAllSensors(ArmingStatus armingStatus) {
        Set<Sensor> sensors = getSensors(true);
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        when(securityRepository.getSensors()).thenReturn(sensors);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.setArmingStatus(armingStatus);
        sensors.forEach(it -> assertFalse(it.getActive()));
    }

    @Test
    void alarmStatus_catShows_alarmStatus() {
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);

        securityService.processImage(mock(BufferedImage.class));
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }
}
