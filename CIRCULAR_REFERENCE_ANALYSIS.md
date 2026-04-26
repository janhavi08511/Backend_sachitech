# JSON Serialization Error Analysis - Circular References

## Error Summary
```
RuntimeException: Could not write JSON: Document nesting depth (1001) exceeds the maximum allowed (1000)
```

## Root Cause: Bidirectional Relationships Without Proper JSON Annotations

Your entities have **circular bidirectional relationships** that cause infinite serialization loops:

### Problem Chain:

1. **User ↔ StudentProfile (Bidirectional)**
   - `User.studentProfile` → `StudentProfile` (OneToOne, mappedBy)
   - `StudentProfile.user` → `User` (OneToOne, owning side)
   - When serializing User → includes StudentProfile → includes User → infinite loop

2. **User ↔ TrainerProfile (Bidirectional)**
   - `User.trainerProfile` → `TrainerProfile` (OneToOne, mappedBy)
   - `TrainerProfile.user` → `User` (OneToOne, owning side)
   - Same circular problem

3. **StudentProfile → FeeManagement → StudentProfile**
   - `StudentProfile` has `enrolledCourses` and `enrolledInternships`
   - `FeeManagement.student` → `StudentProfile`
   - Creates nested circular references

4. **Course → TrainerProfile → User → StudentProfile → FeeManagement → Course**
   - Multiple interconnected circular paths

## Why GET APIs Fail

When AdminController returns entities directly:
- `@GetMapping("/users")` → returns `List<User>`
- Jackson tries to serialize User → includes StudentProfile → includes User → **INFINITE LOOP**
- After 1000 levels of nesting, Jackson throws the error

## Affected GET APIs in AdminController

1. ❌ `GET /api/admin/users` - Returns `List<User>` (has circular refs)
2. ❌ `GET /api/admin/courses` - Returns `List<Course>` (Course → TrainerProfile → User → StudentProfile)
3. ❌ `GET /api/admin/batches` - Returns `List<Batch>` (Batch → User/Course with circular refs)
4. ❌ `GET /api/admin/internships` - Returns `List<Internship>` (similar circular issues)
5. ✅ `GET /api/admin/studentdata/all` - Works (uses custom Map, avoids entities)
6. ✅ `GET /api/admin/fees/student-status` - Works (uses custom Map, avoids entities)

## Solutions

### Solution 1: Add @JsonIgnore to Break Cycles (QUICK FIX)
Add `@JsonIgnore` to the non-owning side of bidirectional relationships.

### Solution 2: Use DTOs (RECOMMENDED)
Create Data Transfer Objects that only include necessary fields, avoiding circular references.

### Solution 3: Use @JsonBackReference/@JsonManagedReference
Explicitly handle bidirectional serialization.

### Solution 4: Configure Jackson Globally
Set max nesting depth higher (not recommended - masks the real problem).

## Implementation Plan

1. **Immediate Fix**: Add `@JsonIgnore` to User entity's bidirectional references
2. **Long-term Fix**: Create DTOs for API responses
3. **Best Practice**: Use DTOs consistently across all endpoints
