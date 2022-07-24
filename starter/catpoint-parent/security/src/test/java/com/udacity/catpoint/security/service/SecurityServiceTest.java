package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.data.*;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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
        Set<Sensor> allSensors = getSensors(false);
        when(securityRepository.getSensors()).thenReturn(allSensors);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        Sensor first = allSensors.iterator().next();
        first.setActive(true);
        securityService.changeSensorActivationStatus(first, false);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    void alarmStatus_alarmActiveAndStatusChanging_notAffected() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));

        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    void alarmStatus_sensorActivateAndPending_alarmState() {
        sensor.setActive(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }
}
