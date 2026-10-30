package com.ztna.controller;

import com.ztna.dto.DeviceRequest;
import com.ztna.model.Device;
import com.ztna.service.DeviceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerDevice(@RequestBody DeviceRequest request) {
        try {
            Device saved = deviceService.registerDevice(
                    request.getUsername(),
                    request.getDeviceName(),
                    request.getDeviceType(),
                    request.getOperatingSystem(),
                    request.getBrowser(),
                    null, // ipAddress — not stored on Device entity
                    request.getStatus());
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Device registration failed: " + e.getMessage());
        }
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<List<Device>> getDevicesByUser(@PathVariable String username) {
        return ResponseEntity.ok(deviceService.getDevicesByUsername(username));
    }

    @PutMapping("/{id}/trust")
    public ResponseEntity<?> setDeviceTrust(
            @PathVariable Long id,
            @RequestParam boolean trusted) {
        try {
            String status = trusted ? "TRUSTED" : "UNREGISTERED";
            Device updated = deviceService.setDeviceStatus(id, status);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDevice(@PathVariable Long id) {
        try {
            deviceService.deleteDevice(id);
            return ResponseEntity.ok("Device deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
